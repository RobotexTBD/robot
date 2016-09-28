package ee.ut.physics.digi.tbd.robot;

import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class ImageProcessor {

    private static final float ORANGE_HUE = AngleUtil.toRadians(10.0f);

    private static long hueSum = 0;
    private static long saturationSum = 0;
    private static long valueSum = 0;
    private static long hueDistanceSum = 0;

    private ImageProcessor() {}

    private static float hueDistance(float hue1, float hue2) {
        if(!Float.isFinite(hue1) || !Float.isFinite(hue2)) {
            return 2 * (float) Math.PI;
        }
        return Math.min(Math.abs(hue1 - hue2), Math.min(hue1, hue2) + 2 * (float) Math.PI - hue2);
    }

    @SneakyThrows
    public static GrayF32 generateCertaintyMap(Planar<GrayF32> hsv) {
        GrayF32 hue = hsv.getBand(0);
        GrayF32 saturation = hsv.getBand(1);
        GrayF32 value = hsv.getBand(2);
        GrayF32 certainty = hue.createSameShape();
        long startTime = System.currentTimeMillis();
        long innerTimeSum = 0;
        hueSum = 0;
        saturationSum = 0;
        valueSum = 0;
        hueDistanceSum = 0;
        for(int y = 0; y < certainty.height; y++) {
            int startPos = certainty.startIndex + y * certainty.stride;
            for(int pos = startPos; pos < startPos + certainty.width; pos++) {
                float hueValue = hue.data[pos];
                float saturationValue = saturation.data[pos];
                float valueValue = value.data[pos];
                long startTimeInner = System.currentTimeMillis();
                float certaintyValue = 255.0f * calculateCertainty(hueValue, saturationValue,
                                                                   valueValue);
                innerTimeSum += System.currentTimeMillis() - startTimeInner;
                certainty.data[pos] = certaintyValue;
            }
        }
        log.debug("hue: " + hueSum + " ms");
        log.debug("saturation: " + saturationSum + " ms");
        log.debug("value: " + valueSum + " ms");
        log.debug("hueDistance: " + hueDistanceSum + " ms");
        log.debug("generateCertaintyMap() new took " + (System.currentTimeMillis() - startTime) + " ms");
        return certainty;
    }

    public static float calculateCertainty(float hue, float saturation, float value) {
        float hueDistance = hueDistance(hue, ORANGE_HUE);
        float result = 1.0f;
        long startTime = System.currentTimeMillis();
        result *= getHueCertainty(value, hueDistance);
        hueSum += System.currentTimeMillis() - startTime;
        startTime = System.currentTimeMillis();
        result *= getSaturationCertainty(saturation);
        saturationSum += System.currentTimeMillis() - startTime;
        startTime = System.currentTimeMillis();
        result *= getValueCertainty(value);
        valueSum += System.currentTimeMillis() - startTime;
        startTime = System.currentTimeMillis();
        result *= getHueDistanceCertainty(hueDistance);
        hueDistanceSum += System.currentTimeMillis() - startTime;
        return result;
    }

    private static float getHueDistanceCertainty(float hueDistance) {
        return Math.max(1.0f - square(hueDistance) * 1.0f, 0.0f);
    }

    private static float getValueCertainty(float value) {
        return limit(0.0f, 1.0f, 4.0f * value - 1.0f);
    }

    private static float getSaturationCertainty(float saturation) {
        return limit(0.0f, 1.0f, 4.0f * saturation - 1.5f);
    }

    private static float getHueCertainty(float value, float hueDistance) {
        float expectedHueDistance = AngleUtil.toRadians(square(10.0f - value / 25.5f));
        float hueError = Math.abs(hueDistance - expectedHueDistance);
        return Math.max(1.0f - square(hueError * 2.25f), 0.0f);
    }

    public static Collection<Blob> findBlobs(GrayF32 certaintyMap) {
        long startTime = System.currentTimeMillis();
        GrayU8 primary = ThresholdImageOps.threshold(certaintyMap, null, 255.0f * 0.9f, false);
        int width = primary.getWidth();
        int height = primary.getHeight();
        int stride = width + 2;
        boolean[] visitedTemplate = buildVisitedMap(primary);
        boolean[] visited = Arrays.copyOf(visitedTemplate, visitedTemplate.length);
        Queue<Integer> visitQueue = new ArrayDeque<>();
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

        System.arraycopy(visitedTemplate, 0, visited, 0, visited.length);
        boolean[] localVisited = new boolean[visited.length];
        Collection<Blob> realBlobs = new ArrayList<>();
        for(Blob blob : preliminaryBlobs) {
            System.arraycopy(visitedTemplate, 0, localVisited, 0, localVisited.length);
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
                certaintyMap.unsafe_set(x, y, 255.0f);
                sumX += x;
                sumY += y;
                size++;
                minX = Math.min(x, minX);
                maxX = Math.max(x, maxX);
                minY = Math.min(y, minY);
                maxY = Math.max(y, maxY);
                visited[position] = true;
                localVisited[position] = true;
                int moves[] = {-1, 0, 1, 0, 0, -1, 0, 1};
                for(int i = 0; i < 4; i++) {
                    int newPosition = position + moves[i * 2] + moves[i * 2 + 1] * stride;
                    if(!visited[newPosition]) {
                        float nextCertainty = certaintyMap.unsafe_get(x + moves[i * 2], y + moves[i * 2 + 1]);
                        if(nextCertainty > 0.5f) {
                            visitQueue.add(newPosition);
                        } else {
                            visited[newPosition] = true;
                        }
                    }
                }
            }
            if(size > 64) { // size may be zero
                for(int x = minX + 1; x <= maxX + 1; x++) {
                    localVisited[x + minY * stride] = true;
                    localVisited[x + (maxY + 2) * stride] = true;

                }
                for(int y = minY + 1; y <= maxY + 1; y++) {
                    localVisited[minX + y * stride] = true;
                    localVisited[maxX + 2 + y * stride] = true;
                }
                visitQueue.add(minX + 1 + (minY + 1) * stride);
                visitQueue.add(maxX + 1 + (minY + 1) * stride);
                visitQueue.add(minX + 1 + (maxY + 1) * stride);
                visitQueue.add(maxX + 1 + (maxY + 1) * stride);
                while(true) {
                    Integer position = visitQueue.poll();
                    if(position == null) {
                        break;
                    }
                    if(localVisited[position]) {
                        continue;
                    }
                    localVisited[position] = true;
                    int[] moves = {-1, 1, -stride, stride};
                    for(int move : moves) {
                        if(!localVisited[position + move]) {
                            visitQueue.add(position + move);
                        }
                    }
                }
                for(int x = minX; x <= maxX; x++) {
                    for(int y = minY; y <= maxY; y++) {
                        if(!localVisited[x + 1 + (y + 1) * stride]) {
                            sumX += x;
                            sumY += y;
                            size++;
                            certaintyMap.unsafe_set(x, y, 255.0f);
                        }
                    }
                }
                realBlobs.add(new Blob(sumX / size, sumY / size, size));
            }
        }
        log.debug("findBlobs() took " + (System.currentTimeMillis() - startTime) + " ms");
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

    private static float square(float a) {
        return a * a;
    }

    private static float limit(float min, float max, float value) {
        return Math.min(Math.max(value, min), max);
    }


}
