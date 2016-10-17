package ee.ut.physics.digi.tbd.robot.util;

import com.github.sarxos.webcam.Webcam;
import ee.ut.physics.digi.tbd.robot.model.ColoredImage;
import lombok.Getter;

import java.nio.ByteBuffer;

public class CameraReader {

    @Getter
    private final Webcam camera;
    private final int width;
    private final int height;
    private ByteBuffer buffer = null;

    public CameraReader(Webcam camera) {
        this.camera = camera;
        width = camera.getViewSize().width;
        height = camera.getViewSize().height;
        buffer = ByteBuffer.allocate(width * height * 3);
    }

    public ColoredImage readRgbImage() {
        buffer.clear();
        camera.getImageBytes(buffer);
        ColoredImage rgbImage = new ColoredImage(width, height);
        int[] data = rgbImage.getData();
        for(int i = 0; i < rgbImage.getElementCount(); i++) {
            data[i] = getNextAsInt() << 16 | getNextAsInt() << 8 | getNextAsInt();
        }
        return rgbImage;
    }

    private int getNextAsInt() {
        return buffer.get() & 0xFF;
    }

}
