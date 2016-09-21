package ee.ut.physics.digi.tbd.robot;

import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayDeque;
import java.util.Queue;

@Slf4j
public class ImageProcessor {

    private static final float ORANGE_HUE = AngleUtil.toRadians(24.0f);

    private ImageProcessor() {}

    private static float hueDistance(float hue1, float hue2) {
        if(!Float.isFinite(hue1) || !Float.isFinite(hue2)) {
            return 2 * (float) Math.PI;
        }
        return Math.min(Math.abs(hue1 - hue2), Math.min(hue1, hue2) + 2 * (float) Math.PI - hue2);
    }

    public static void mutateImage(Planar<GrayF32> hsv) {
        GrayF32 hue = hsv.getBand(0);
        GrayF32 saturation = hsv.getBand(1);
        GrayF32 value = hsv.getBand(2);
        float orangeHue = 0.4f;
        for(int x = 0; x < hue.getWidth(); x++) {
            for(int y = 0; y < hue.getHeight(); y++) {
                float hueDistance = hueDistance(orangeHue, hue.get(x, y));
                if(hueDistance > 0.2f || value.get(x, y) * saturation.get(x, y) < 0.4f) {
                    value.set(x, y, 0);
                    saturation.set(x, y, 0);
                } else {
                    saturation.set(x, y, 1);
                }
            }
        }
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
                float certaintyValue = hueCertainty * saturationCertainty * valueCertainty;
                certainty.unsafe_set(x, y, 510.0f * certaintyValue - 255.0f);
            }
        }
        return certainty;
    }

    public static void fillHoles(GrayU8 binary) {
        long startTime = System.currentTimeMillis();
        int width = binary.getWidth();
        int height = binary.getHeight();
        boolean[][] visited = new boolean[binary.getWidth()][binary.getHeight()];
        Queue<Integer> visitQueue = new ArrayDeque<>();
        for(int x = 0; x < binary.getWidth(); x++) {
            visitQueue.add(x);
            visitQueue.add(x + width * (height - 1));
        }
        for(int y = 0; y < binary.getHeight(); y++) {
            visitQueue.add(width * y);
            visitQueue.add(width * (y + 1) - 1);
        }
        while(true) {
            Integer position = visitQueue.poll();
            if(position == null) {
                break;
            }
            int x = position % width;
            int y = position / width;
            if(visited[x][y] || binary.unsafe_get(x, y) > 0) {
                continue;
            }
            visited[x][y] = true;
            if(x > 0) {
                if(!visited[x - 1][y] && binary.unsafe_get(x - 1, y) == 0)
                visitQueue.add(position - 1);
            }
            if(x < width - 1) {
                if(!visited[x + 1][y] && binary.unsafe_get(x + 1, y) == 0)
                visitQueue.add(position + 1);
            }
            if(y > 0) {
                if(!visited[x][y - 1] && binary.unsafe_get(x, y - 1) == 0)
                visitQueue.add(position - width);
            }
            if(y < height - 1) {
                if(!visited[x][y + 1] && binary.unsafe_get(x, y + 1) == 0)
                visitQueue.add(position + width);
            }
        }
        log.debug("Graph took " + (System.currentTimeMillis() - startTime) + " ms");
        for(int x = 0; x < width; x++) {
            for(int y = 0; y < height; y++) {
                if(!visited[x][y]) {
                    binary.unsafe_set(x, y, 1);
                }
            }
        }
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
