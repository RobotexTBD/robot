package ee.ut.physics.digi.tbd.robot;

import boofcv.io.webcamcapture.UtilWebcamCapture;
import ee.ut.physics.digi.tbd.robot.kernel.BallDetectorKernel;
import ee.ut.physics.digi.tbd.robot.kernel.RgbToHsvConverterKernel;
import ee.ut.physics.digi.tbd.robot.kernel.ThresholderKernel;
import ee.ut.physics.digi.tbd.robot.mainboard.*;
import ee.ut.physics.digi.tbd.robot.model.BinaryImage;
import ee.ut.physics.digi.tbd.robot.model.ColoredImage;
import ee.ut.physics.digi.tbd.robot.model.GrayscaleImage;
import ee.ut.physics.digi.tbd.robot.util.CameraReader;
import javafx.application.Platform;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;

@Slf4j
public class Robot implements Runnable {

    private static final boolean DEBUG = true;

    private DebugWindow debugWindow;
    private final Mainboard mainboard;
    private final CameraReader cameraReader;
    private final RgbToHsvConverterKernel rgbToHsvConverterKernel;
    private final BallDetectorKernel ballDetectorKernel;
    private final ThresholderKernel thresholderKernel;

    public Robot(String cameraName, int width, int height) throws IOException {
        if(isDebug()) {
            debugWindow = DebugWindow.getInstance();
        }
        mainboard = new MainboardImpl();
        cameraReader = new CameraReader(UtilWebcamCapture.openDevice(cameraName, width, height));
        rgbToHsvConverterKernel = new RgbToHsvConverterKernel(width, height);
        ballDetectorKernel = new BallDetectorKernel(width, height);
        thresholderKernel = new ThresholderKernel(width, height);
    }

    public static void main(String[] args) throws IOException {
        new Robot("ManyCam", 640, 480).run();
    }

    public boolean isDebug() {
        return DEBUG;
    }

    @SuppressWarnings("InfiniteLoopStatement")
    @Override
    @SneakyThrows
    public void run() {
        BufferedReader a = new BufferedReader(new InputStreamReader(((MainboardImpl)mainboard).serialPort.getInputStream()));
        while(true) {
            ColoredImage rgbImage = cameraReader.readRgbImage();
            long startTime = System.currentTimeMillis();
            loop(rgbImage);
            //log.debug("Loop took " + (System.currentTimeMillis() - startTime) + " ms");
            //Thread.sleep(1000);
            if(isDebug()) {
                Platform.runLater(debugWindow::render);
            }
        }
    }

    public void loop(ColoredImage rgbImage) {
        ColoredImage hsvImage = rgbToHsvConverterKernel.convert(rgbImage);
        debugWindow.setImage(ImagePanel.ORIGINAL, rgbImage, "Original");
        GrayscaleImage certaintyMap = ballDetectorKernel.generateCertaintyMap(hsvImage);
        debugWindow.setImage(ImagePanel.MUTATED1, certaintyMap, "Certainty map");
        BinaryImage maxCertainty = thresholderKernel.threshold(certaintyMap, 205, 255);
        debugWindow.setImage(ImagePanel.MUTATED2, maxCertainty, "Max certainty");
        Collection<Blob> blobs = BallDetector.findBalls(certaintyMap, maxCertainty);
        for(Blob blob : blobs) {
            if(260 < blob.getCenterX() && blob.getCenterX() < 380) {
                moveForward();
                return;
            }
        }
        turnRight();
    }

    private void turnRight() {
        log.warn("TURN RIGHT!");
        float speed = 0.5f;
        mainboard.setSpeed(Motor.LEFT, speed, Direction.BACK);
        mainboard.setSpeed(Motor.RIGHT, speed, Direction.FORWARD);
        mainboard.setSpeed(Motor.BACK, speed, Direction.RIGHT);
    }

    private void moveForward() {
        log.warn("MOVE FORWARD!");
        float speed = 0.5f;
        mainboard.setSpeed(Motor.LEFT, speed, Direction.FORWARD);
        mainboard.setSpeed(Motor.RIGHT, speed, Direction.FORWARD);
        mainboard.setSpeed(Motor.BACK, 0.0f, Direction.NONE);
    }

}
