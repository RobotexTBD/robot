package ee.ut.physics.digi.tbd.robot;

import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BiConsumer;

@Slf4j
public class ImageProcessor {

    private static final float ORANGE_HUE = AngleUtil.toRadians(18.0f);

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

    public static Collection<Blob> findBlobsAndFillHoles(GrayU8 binary) {
        long startTime = System.currentTimeMillis();
        int width = binary.getWidth();
        int height = binary.getHeight();
        int stride = width + 2;
        boolean[][] visited = buildVisitedMap(binary);
        Queue<Integer> visitQueue = new ArrayDeque<>();
        for(int x = 1; x <= binary.getWidth(); x++) {
            visitQueue.add(x + stride);
            visitQueue.add(x + stride * height);
        }
        for(int y = 2; y <= binary.getHeight() - 1; y++) {
            visitQueue.add(1 + stride * y);
            visitQueue.add(width + stride * y);
        }
        traverse(visited, visitQueue, null);
        for(int x = 0; x < width; x++) {
            for(int y = 0; y < height; y++) {
                if(binary.unsafe_get(x, y) > 0) {
                    visited[x + 1][y + 1] = false;
                }
            }
        }
        Collection<Blob> blobs = new ArrayList<>();
        for(int x = 0; x < width; x++) {
            for(int y = 0; y < height; y++) {
                if(!visited[x + 1][y + 1]) {
                    LongAdder sumX = new LongAdder();
                    LongAdder sumY = new LongAdder();
                    LongAdder countAdder = new LongAdder();
                    visitQueue.add(x + 1 + (y + 1) * stride);
                    traverse(visited, visitQueue, (matchX, matchY) -> {
                        binary.unsafe_set(matchX - 1, matchY - 1, 1);
                        sumX.add(matchX);
                        //noinspection SuspiciousNameCombination
                        sumY.add(matchY);
                        countAdder.increment();
                    });
                    int count = countAdder.intValue();
                    if(count > 64) {
                        blobs.add(new Blob(sumX.intValue() / count, sumY.intValue() / count, count));
                    }
                }
            }
        }
        log.debug("Image processing took " + (System.currentTimeMillis() - startTime) + " ms");
        return blobs;
    }

    private static void traverse(boolean[][] visited, Queue<Integer> visitQueue,
                                BiConsumer<Integer, Integer> matchConsumer) {
        int stride = visited.length;
        while(true) {
            Integer position = visitQueue.poll();
            if(position == null) {
                break;
            }
            int x = position % stride;
            int y = position / stride;
            if(visited[x][y]) {
                continue;
            }
            visited[x][y] = true;
            if(matchConsumer != null) {
                matchConsumer.accept(x, y);
            }
            if(!visited[x - 1][y]) {
                visitQueue.add(position - 1);
            }
            if(!visited[x + 1][y]) {
                visitQueue.add(position + 1);
            }
            if(!visited[x][y - 1]) {
                visitQueue.add(position - stride);
            }
            if(!visited[x][y + 1]) {
                visitQueue.add(position + stride);
            }
        }
    }

    private static boolean[][] buildVisitedMap(GrayU8 image) {
        int width = image.getWidth();
        int height = image.getHeight();
        boolean[][] visited = new boolean[width + 2][height + 2];
        for(int x = 0; x < width + 2; x++) {
            visited[x][0] = true;
            visited[x][height + 1] = true;
        }
        for(int y = 1; y <= height; y++) {
            visited[0][y] = true;
            visited[width + 1][y] = true;
        }
        for(int x = 0; x < width; x++) {
            for(int y = 0; y < height; y++) {
                visited[x + 1][y + 1] = image.unsafe_get(x, y) > 0;
            }
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
