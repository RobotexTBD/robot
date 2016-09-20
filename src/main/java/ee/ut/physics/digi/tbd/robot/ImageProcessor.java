package ee.ut.physics.digi.tbd.robot;

import boofcv.struct.image.GrayF32;
import boofcv.struct.image.Planar;

public class ImageProcessor {

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
                float hueDistance = hueDistance(orangeHue, hue.unsafe_get(x, y));
                if(hueDistance > 0.2f || value.unsafe_get(x, y) * saturation.unsafe_get(x, y) < 0.4f) {
                    value.unsafe_set(x, y, 0);
                    saturation.unsafe_set(x, y, 0);
                } else {
                    saturation.unsafe_set(x, y, 1);
                }
            }
        }
    }

}
