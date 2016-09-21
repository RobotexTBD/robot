package ee.ut.physics.digi.tbd.robot;

import boofcv.alg.color.ColorHsv;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;
import ee.ut.physics.digi.tbd.robot.colorspace.HSL;
import ee.ut.physics.digi.tbd.robot.colorspace.HSV;

import java.awt.image.BufferedImage;

public class ImageConverterUtil {

    private ImageConverterUtil() {}

    public static Planar<GrayF32> getHsvImage(BufferedImage image) {
        Planar<GrayF32> rgb = ConvertBufferedImage.convertFromMulti(getRgbBufferedImage(image), null, true,
                                                                    GrayF32.class);
        Planar<GrayF32> hsv = rgb.createSameShape();
        ColorHsv.rgbToHsv_F32(rgb, hsv);
        return hsv;
    }

    public static BufferedImage getBufferedImage(Planar<GrayF32> hsv) {
        Planar<GrayF32> rgb = hsv.createSameShape();
        ColorHsv.hsvToRgb_F32(hsv, rgb);
        BufferedImage bufferedImage = new BufferedImage(hsv.getWidth(), hsv.getHeight(), BufferedImage.TYPE_INT_RGB);
        ConvertBufferedImage.convertTo_F32(rgb, bufferedImage, true);
        return bufferedImage;
    }

    public static BufferedImage getBufferedImage(GrayU8 bw) {
        return VisualizeBinaryData.renderBinary(bw, false, null);
    }

    public static BufferedImage getBufferedImage(GrayF32 bw) {
        return VisualizeImageData.graySign(bw, null, 255.0f);
    }

    private static BufferedImage getRgbBufferedImage(BufferedImage image) {
        BufferedImage convertedImg = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        convertedImg.getGraphics().drawImage(image, 0, 0, null);
        return convertedImg;
    }

    @Deprecated
    //https://gist.github.com/xpansive/1337890
    private static Planar<GrayF32> hsvToHsl(Planar<GrayF32> hsv) {
        Planar<GrayF32> hsl = hsv.createSameShape();
        for(int x = 0; x < hsv.getWidth(); x++) {
            for(int y = 0; y < hsv.getHeight(); y++) {
                float hue = getValue(hsv, HSV.hue, x, y);
                float saturation = getValue(hsv, HSV.saturation, x, y);
                float value = getValue(hsv, HSV.value, x, y);
                float newHue = (2 - saturation) * value;
                setValue(hsl, HSL.hue, x, y, hue);
                setValue(hsl, HSL.saturation, x, y, saturation * value / (newHue < 1 ? newHue : 2 - newHue));
                setValue(hsl, HSL.lightness, x, y, newHue / 2);
            }
        }
        return hsl;
    }

    private static float getValue(Planar<GrayF32> hsv, HSV band, int x, int y) {
        return hsv.getBand(band.getIndex()).unsafe_get(x, y);
    }

    private static float getValue(Planar<GrayF32> hsl, HSL band, int x, int y) {
        return hsl.getBand(band.getIndex()).unsafe_get(x, y);
    }

    private static void setValue(Planar<GrayF32> hsv, HSV band, int x, int y, float value) {
        hsv.getBand(band.getIndex()).unsafe_set(x, y, value);
    }


    private static void setValue(Planar<GrayF32> hsl, HSL band, int x, int y, float value) {
        hsl.getBand(band.getIndex()).unsafe_set(x, y, value);
    }


}
