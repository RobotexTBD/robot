package ee.ut.physics.digi.tbd.robot;

import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.atomic.LongAdder;

@Slf4j
public class ImageProcessor {

    private static final float ORANGE_HUE = AngleUtil.toRadians(15.0f);

    private ImageProcessor() {}

    private static float hueDistance(float hue1, float hue2) {
        if(!Float.isFinite(hue1) || !Float.isFinite(hue2)) {
            return 2 * (float) Math.PI;
        }
        return Math.min(Math.abs(hue1 - hue2), Math.min(hue1, hue2) + 2 * (float) Math.PI - hue2);
    }

    public static GrayF32 generateCertaintyMap(Planar<GrayF32> hsv) {
        GrayF32 hue = hsv.getBand(0);
        GrayF32 saturation = hsv.getBand(1);
        GrayF32 value = hsv.getBand(2);
        GrayF32 certainty = saturation.createSameShape();
        for(int x = 0; x < hue.getWidth(); x++) {
            for(int y = 0; y < hue.getHeight(); y++) {
                float expectedHueDistance = AngleUtil.toRadians(pow(10.0f - value.unsafe_get(x, y) / 25.5f, 2.25f));
                float hueDistance = hueDistance(hue.unsafe_get(x, y), ORANGE_HUE);
                float hueError = Math.abs(hueDistance - expectedHueDistance);
                float hueCertainty = Math.max(1.0f - pow(hueError * 2.5f, 2.25f), 0.0f);
                float saturationCertainty = limit(0.0f, 1.0f, 4 * saturation.unsafe_get(x, y) - 1.5f);
                float valueCertainty = limit(0.0f, 1.0f, 4 * saturation.unsafe_get(x, y) - 1.0f);
                float hueDistanceCertainty = Math.max(1.0f - pow(hueDistance, 2.25f) * 1.5f, 0.0f);
                float certaintyValue = hueCertainty * saturationCertainty * valueCertainty * hueDistanceCertainty;
                certainty.unsafe_set(x, y, 255.0f * certaintyValue);
            }
        }
        return certainty;
    }

    public static Collection<Blob> findBlobs(GrayF32 certaintyMap) {
        long startTime = System.currentTimeMillis();
        GrayU8 primary = ThresholdImageOps.threshold(certaintyMap, null, 255.0f * 0.9f, false);
        int width = primary.getWidth();
        int height = primary.getHeight();
        int stride = width + 2;
        boolean[] visited = buildVisitedMap(primary);
        Queue<Integer> visitQueue = new ArrayDeque<>();
        int a = 0;
        for(int x = 0; x < width; x++) {
            for(int y = 0; y < height; y++) {
                if(primary.unsafe_get(x, y) == 0) {
                    visited[x + 1 + (y + 1) * stride] = true;
                }
            }
        }
        Collection<Blob> preliminaryBlobs = new ArrayList<>();
        for(int x = 0; x < width; x++) {
            for(int y = 0; y < height; y++) {
                if(primary.unsafe_get(x, y) != 0 && !visited[x + 1 + (y + 1) * stride]) {
                    visitQueue.add(x + 1 + (y + 1) * stride);
                    int size = getPrimaryBlobSize(visited, visitQueue, stride);
                    if(size > 16) {
                        preliminaryBlobs.add(new Blob(x, y, size));
                    }
                }
            }
        }
        visited = buildVisitedMap(primary);
        Collection<Blob> realBlobs = new ArrayList<>();
        for(Blob blob : preliminaryBlobs) {
            int minX = blob.getCenterX();
            int maxX = blob.getCenterX();
            int minY = blob.getCenterY();
            int maxY = blob.getCenterY();
            visitQueue.add(blob.getCenterX() + 1 + (blob.getCenterY() + 1) * stride);
            int sumX = 0;
            int sumY = 0;
            int size = 0;
            while(true) {
                Integer position = visitQueue.poll();
                if(position == null) {
                    break;
                }
                if(visited[position]) {
                    continue;
                }
                int x = (position - 1) % stride;
                int y = (position - 1) / stride - 1;
                sumX += x;
                sumY += y;
                size++;
                minX = Math.min(x, minX);
                maxX = Math.max(x, maxX);
                minY = Math.min(y, minY);
                maxY = Math.max(y, maxY);
                visited[position] = true;
                int moves[] = {-1, 0, 1, 0, 0, -1, 0, 1};
                for(int i = 0; i < 4; i++) {
                    int newPosition = position + moves[i * 2] + moves[i * 2 + 1] * stride;
                    if(!visited[newPosition]) {
                        float nextCertainty = certaintyMap.unsafe_get(x + moves[i * 2], y + moves[i * 2 + 1]);
                        if(nextCertainty > 0.5f) {
                            visitQueue.add(newPosition);
                        }
                    }
                }
            }
            if(size > 64) { // size may be zero
                realBlobs.add(new Blob(sumX / size, sumY / size, size));
            }
        }
        log.debug("Image processing took " + (System.currentTimeMillis() - startTime) + " ms");
        return realBlobs;
    }

    private static int getPrimaryBlobSize(boolean[] visited, Queue<Integer> visitQueue, int stride) {
        int size = 0;
        while(true) {
            Integer position = visitQueue.poll();
            if(position == null) {
                break;
            }
            if(visited[position]) {
                continue;
            }
            size++;
            visited[position] = true;
            for(int move : new int[] {-1, 1, -stride, stride}) {
                if(!visited[position + move]) {
                    visitQueue.add(position + move);
                }
            }
        }
        return size;
    }

    private static boolean[] buildVisitedMap(GrayU8 image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int stride = width + 2;
        boolean[] visited = new boolean[(width + 2) * (height + 2)];
        for(int x = 0; x < width + 2; x++) {
            visited[x] = true;
            visited[x + (height + 1) * stride] = true;
        }
        for(int y = 1; y <= height; y++) {
            visited[y * stride] = true;
            visited[width + 1 + y * stride] = true;
        }
        return visited;
    }

    private static float distanceFromRange(float min, float max, float value) {
        if(value < min) {
            return min - value;
        }
        if(value > max) {
            return value - max;
        }
        return 0.0f;
    }

    private static float pow(float a, float b) {
        return (float) Math.pow(a, b);
    }

    private static float limit(float min, float max, float value) {
        return Math.min(Math.max(value, min), max);
    }


}
