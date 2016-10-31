package ee.ut.physics.digi.tbd.robot;

import boofcv.io.webcamcapture.UtilWebcamCapture;
import com.fazecast.jSerialComm.SerialPort;
import ee.ut.physics.digi.tbd.robot.debug.DebugWindow;
import ee.ut.physics.digi.tbd.robot.debug.ImagePanel;
import ee.ut.physics.digi.tbd.robot.kernel.ImageProcessorService;
import ee.ut.physics.digi.tbd.robot.mainboard.Direction;
import ee.ut.physics.digi.tbd.robot.mainboard.Mainboard;
import ee.ut.physics.digi.tbd.robot.mainboard.MainboardFactory;
import ee.ut.physics.digi.tbd.robot.mainboard.Motor;
import ee.ut.physics.digi.tbd.robot.mainboard.command.MotorSpeedCommand;
import ee.ut.physics.digi.tbd.robot.mainboard.command.MotorStopCommand;
import ee.ut.physics.digi.tbd.robot.matrix.image.BinaryImage;
import ee.ut.physics.digi.tbd.robot.matrix.image.ColoredImage;
import ee.ut.physics.digi.tbd.robot.matrix.image.GrayscaleImage;
import ee.ut.physics.digi.tbd.robot.referee.Referee;
import ee.ut.physics.digi.tbd.robot.referee.RefereeFactory;
import ee.ut.physics.digi.tbd.robot.referee.RefereeListener;
import ee.ut.physics.digi.tbd.robot.util.CameraReader;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
public class Robot implements Runnable {

    private static final boolean DEBUG = false;

    private DebugWindow debugWindow;
    private final Mainboard mainboard;
    private final CameraReader cameraReader;
    private final BallDetector ballDetector;
    private final ImageProcessorService imageProcessorService;
    private final Referee referee;

    public Robot(String cameraName, int width, int height) throws IOException {
        if(isDebug()) {
            debugWindow = DebugWindow.getInstance();
        }
        logAvailableSerialPorts();
        mainboard = MainboardFactory.getInstance().getMainboard();
        cameraReader = new CameraReader(UtilWebcamCapture.openDevice(cameraName, width, height));
        imageProcessorService = new ImageProcessorService(width, height);
        ballDetector = new BallDetector(imageProcessorService);
        referee = RefereeFactory.getInstance().getReferee();
    }

    private void logAvailableSerialPorts() {
        log.debug("Found " + SerialPort.getCommPorts().length + " serial ports: " +
                  Arrays.stream(SerialPort.getCommPorts())
                        .map(SerialPort::getDescriptivePortName)
                        .collect(Collectors.joining(", ")));
    }

    public static void main(String[] args) throws IOException {
        new Robot(Settings.getInstance().getWebcamName(), 640, 480).run();
    }

    public boolean isDebug() {
        return DEBUG;
    }

    @SuppressWarnings("InfiniteLoopStatement")
    @Override
    public void run() {
        final AtomicBoolean running = new AtomicBoolean(false);
        referee.registerListener(new RefereeListener() {
            @Override
            public void onStart() {
                log.debug("Game started");
                running.set(true);
            }

            @Override
            public void onStop() {
                log.debug("Game stopped");
                running.set(false);
            }
        });
        while(true) {
            if(!running.get()) {
                continue;
            }
            ColoredImage rgbImage = cameraReader.readRgbImage();
            long startTime = System.currentTimeMillis();
            loop(rgbImage);
            log.trace("Loop took " + (System.currentTimeMillis() - startTime) + " ms");
            if(isDebug()) {
                Platform.runLater(debugWindow::render);
            }
        }
    }

    public void loop(ColoredImage rgbImage) {
        ColoredImage hsvImage = imageProcessorService.convertRgbToHsv(rgbImage);
        if(isDebug()) {
            debugWindow.setImage(ImagePanel.ORIGINAL, rgbImage, "Original");
            GrayscaleImage certaintyMap = imageProcessorService.generateBallCertaintyMap(hsvImage);
            debugWindow.setImage(ImagePanel.MUTATED1, certaintyMap, "Certainty map");
            BinaryImage maxCertainty = imageProcessorService.threshold(certaintyMap, 205, 255);
            debugWindow.setImage(ImagePanel.MUTATED2, maxCertainty, "Max certainty");
        }
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
        log.trace("TURN RIGHT!");
        float speed = 0.5f;
        mainboard.sendCommandsBatch(new MotorSpeedCommand(Motor.LEFT, speed, Direction.BACK),
                                    new MotorSpeedCommand(Motor.RIGHT, speed, Direction.FORWARD),
                                    new MotorSpeedCommand(Motor.BACK, speed, Direction.RIGHT));
    }

    private void moveForward() {
        log.trace("MOVE FORWARD!");
        float speed = 0.5f;
        mainboard.sendCommandsBatch(new MotorSpeedCommand(Motor.LEFT, speed, Direction.FORWARD),
                                    new MotorSpeedCommand(Motor.RIGHT, speed, Direction.FORWARD),
                                    new MotorStopCommand(Motor.BACK));
    }

}
