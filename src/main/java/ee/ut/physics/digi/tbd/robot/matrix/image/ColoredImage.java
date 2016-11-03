package ee.ut.physics.digi.tbd.robot.matrix.image;

import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import lombok.Getter;

@Getter
public class ColoredImage implements Image {

    public static final int ALPHA_OPAQUE = 0xFF000000;
    private final int[] data;
    private final int width;
    private final int height;

    public ColoredImage(int width, int height) {
        data = new int[width * height];
        this.width = width;
        this.height = height;
    }

    @Override
    public WritableImage toWritableImage() {
        WritableImage image = new WritableImage(width, height);
        PixelWriter pixelWriter = image.getPixelWriter();
        int index = 0;
        for(int y = 0; y < height; y++) {
            for(int x = 0; x < width; x++, index++) {
                pixelWriter.setArgb(x, y, ALPHA_OPAQUE | data[index]);
            }
        }
        return image;
    }

}
