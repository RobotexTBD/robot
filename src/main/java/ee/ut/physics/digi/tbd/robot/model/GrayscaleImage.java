package ee.ut.physics.digi.tbd.robot.model;

import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import lombok.Getter;

@Getter
public class GrayscaleImage implements Image {

    private final int width;
    private final int height;
    private final int[] data;

    public GrayscaleImage(int width, int height) {
        this.width = width;
        this.height = height;
        data = new int[width * height];
    }

    @Override
    public WritableImage toWritableImage() {
        WritableImage image = new WritableImage(width, height);
        PixelWriter pixelWriter = image.getPixelWriter();
        int index = 0;
        for(int y = 0; y < height; y++) {
            for(int x = 0; x < width; x++, index++) {
                pixelWriter.setArgb(x, y, 0xFF000000 | data[index] << 16 | data[index] << 8 | data[index]);
            }
        }
        return image;
    }

}
