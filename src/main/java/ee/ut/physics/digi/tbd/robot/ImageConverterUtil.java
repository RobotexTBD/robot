package ee.ut.physics.digi.tbd.robot;

import boofcv.alg.color.ColorHsv;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;

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

    private static BufferedImage getRgbBufferedImage(BufferedImage image) {
        BufferedImage convertedImg = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        convertedImg.getGraphics().drawImage(image, 0, 0, null);
        return convertedImg;
    }


}
