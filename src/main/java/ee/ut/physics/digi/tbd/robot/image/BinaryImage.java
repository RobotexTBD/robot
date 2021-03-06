package ee.ut.physics.digi.tbd.robot.image;

import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import lombok.Getter;

@Getter
public class BinaryImage implements Image {

    public static final int ARGB_WHITE = 0xFFFFFFFF;
    public static final int ARGB_BLACK = 0xFF000000;
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
                pixelWriter.setArgb(x, y, data[index] ? ARGB_WHITE : ARGB_BLACK);
            }
        }
        return image;
    }

}
