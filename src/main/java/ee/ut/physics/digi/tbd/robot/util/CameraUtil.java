package ee.ut.physics.digi.tbd.robot.util;

import com.github.sarxos.webcam.Webcam;

import java.awt.*;

public final class CameraUtil {

    private CameraUtil() {}

    public static Webcam openCamera(String nameFragment, int width, int height) {
        Webcam camera = Webcam.getWebcams().stream()
                                      .filter(c -> c.getName().contains(nameFragment))
                                      .findAny()
                                      .orElseThrow(() -> new IllegalArgumentException("Cannot find webcam \"" +
                                                                                      nameFragment + "\""));

        camera.setViewSize(new Dimension(width, height));
        camera.open();
        return camera;
    }

}
