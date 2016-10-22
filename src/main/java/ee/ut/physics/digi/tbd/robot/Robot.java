package ee.ut.physics.digi.tbd.robot;

import boofcv.io.webcamcapture.UtilWebcamCapture;
import ee.ut.physics.digi.tbd.robot.debug.DebugWindow;
import ee.ut.physics.digi.tbd.robot.debug.ImagePanel;
import ee.ut.physics.digi.tbd.robot.kernel.BallDetectorKernel;
import ee.ut.physics.digi.tbd.robot.kernel.RgbToHsvConverterKernel;
import ee.ut.physics.digi.tbd.robot.kernel.ThresholderKernel;
import ee.ut.physics.digi.tbd.robot.mainboard.*;
import ee.ut.physics.digi.tbd.robot.mainboard.command.MotorSpeedCommand;
import ee.ut.physics.digi.tbd.robot.mainboard.command.MotorStopCommand;
import ee.ut.physics.digi.tbd.robot.matrix.image.BinaryImage;
import ee.ut.physics.digi.tbd.robot.matrix.image.ColoredImage;
import ee.ut.physics.digi.tbd.robot.matrix.image.GrayscaleImage;
import ee.ut.physics.digi.tbd.robot.util.CameraReader;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
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
    private final BallDetector ballDetector;

    public Robot(String cameraName, int width, int height) throws IOException {
        if(isDebug()) {
            debugWindow = DebugWindow.getInstance();
        }
        mainboard = new MainboardImpl();
        cameraReader = new CameraReader(UtilWebcamCapture.openDevice(cameraName, width, height));
        rgbToHsvConverterKernel = new RgbToHsvConverterKernel(width, height);
        ballDetectorKernel = new BallDetectorKernel(width, height);
        thresholderKernel = new ThresholderKernel(width, height);
        ballDetector = new BallDetector(ballDetectorKernel, thresholderKernel);
    }

    public static void main(String[] args) throws IOException {
        new Robot("ManyCam", 640, 480).run();
    }

    public boolean isDebug() {
        return DEBUG;
    }

    @SuppressWarnings("InfiniteLoopStatement")
    @Override
    public void run() {
        while(true) {
            ColoredImage rgbImage = cameraReader.readRgbImage();
            long startTime = System.currentTimeMillis();
            loop(rgbImage);
            log.debug("Loop took " + (System.currentTimeMillis() - startTime) + " ms");
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
        Collection<Blob> blobs = ballDetector.findBalls(hsvImage);
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
        mainboard.sendCommand(new MotorSpeedCommand(Motor.LEFT, speed, Direction.BACK));
        mainboard.sendCommand(new MotorSpeedCommand(Motor.RIGHT, speed, Direction.FORWARD));
        mainboard.sendCommand(new MotorSpeedCommand(Motor.BACK, speed, Direction.RIGHT));
    }

    private void moveForward() {
        log.warn("MOVE FORWARD!");
        float speed = 0.5f;
        mainboard.sendCommand(new MotorSpeedCommand(Motor.LEFT, speed, Direction.FORWARD));
        mainboard.sendCommand(new MotorSpeedCommand(Motor.RIGHT, speed, Direction.FORWARD));
        mainboard.sendCommand(new MotorStopCommand(Motor.BACK));
    }

}
