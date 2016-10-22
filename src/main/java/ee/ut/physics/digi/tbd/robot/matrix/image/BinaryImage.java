package ee.ut.physics.digi.tbd.robot.matrix.image;

import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import lombok.Getter;

@Getter
public class BinaryImage implements Image {

    private final int width;
    private final int height;
    private final boolean[] data;

    public BinaryImage(int width, int height) {
        this.width = width;
        this.height = height;
        this.data = new boolean[width * height];
    }

    @Override
    public WritableImage toWritableImage() {
        WritableImage image = new WritableImage(width, height);
        PixelWriter pixelWriter = image.getPixelWriter();
        int index = 0;
        for(int y = 0; y < height; y++) {
            for(int x = 0; x < width; x++, index++) {
                pixelWriter.setArgb(x, y, data[index] ? 0xFFFFFFFF : 0xFF000000);
            }
        }
        return image;
    }

}
