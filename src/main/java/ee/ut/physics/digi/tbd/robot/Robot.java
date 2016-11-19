package ee.ut.physics.digi.tbd.robot;

import com.github.sarxos.webcam.Webcam;
import ee.ut.physics.digi.tbd.robot.debug.DebugWindow;
import ee.ut.physics.digi.tbd.robot.debug.ImagePanel;
import ee.ut.physics.digi.tbd.robot.factory.MainboardFactory;
import ee.ut.physics.digi.tbd.robot.factory.RefereeFactory;
import ee.ut.physics.digi.tbd.robot.image.ColoredImage;
import ee.ut.physics.digi.tbd.robot.image.GrayscaleImage;
import ee.ut.physics.digi.tbd.robot.image.blob.Blob;
import ee.ut.physics.digi.tbd.robot.image.processing.ImageProcessorService;
import ee.ut.physics.digi.tbd.robot.image.processing.detector.BallDetector;
import ee.ut.physics.digi.tbd.robot.image.processing.detector.GoalDetector;
import ee.ut.physics.digi.tbd.robot.logic.RobotBehaviour;
import ee.ut.physics.digi.tbd.robot.logic.RobotBehaviourFactory;
import ee.ut.physics.digi.tbd.robot.logic.state.GameObject;
import ee.ut.physics.digi.tbd.robot.logic.state.GameObjectType;
import ee.ut.physics.digi.tbd.robot.logic.state.MainboardState;
import ee.ut.physics.digi.tbd.robot.logic.state.RobotState;
import ee.ut.physics.digi.tbd.robot.mainboard.Mainboard;
import ee.ut.physics.digi.tbd.robot.referee.Referee;
import ee.ut.physics.digi.tbd.robot.referee.RefereeListener;
import ee.ut.physics.digi.tbd.robot.util.CameraReader;
import ee.ut.physics.digi.tbd.robot.util.CameraUtil;
import javafx.application.Platform;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
public class Robot implements Runnable {

    private static final int WEBCAM_WIDTH = 640;
    private static final int WEBCAM_HEIGHT = 480;

    private String state = "searching";

    private DebugWindow debugWindow;
    private final CameraReader cameraReader;
    private final BallDetector ballDetector;
    private final GoalDetector goalDetector;
    private final ImageProcessorService imageProcessorService;
    private final Mainboard mainboard;
    private final Referee referee;
    private final Settings settings;
    private final RobotBehaviour behaviour;

    public Robot(Webcam camera) throws IOException {
        settings = Settings.getInstance();
        if(isDebug()) {
            debugWindow = DebugWindow.getInstance();
        }
        cameraReader = new CameraReader(camera);
        imageProcessorService = new ImageProcessorService(camera.getViewSize().width, camera.getViewSize().height);
        ballDetector = new BallDetector();
        goalDetector = new GoalDetector();
        mainboard = MainboardFactory.getInstance().getMainboard();
        referee = RefereeFactory.getInstance().getReferee();
        behaviour = RobotBehaviourFactory.getInstance().getBehaviour();
        behaviour.setMainboard(mainboard);
    }


    public static void main(String[] args) throws IOException {
        //TODO: refactor to use factory
        Webcam camera = CameraUtil.openCamera(Settings.getInstance().getWebcamName(), WEBCAM_WIDTH, WEBCAM_HEIGHT);
        new Robot(camera).run();
    }

    public boolean isDebug() {
        return settings.shouldShowDebugWindow();
    }

    @SuppressWarnings("InfiniteLoopStatement")
    @Override
    public void run() {
        final AtomicBoolean running = new AtomicBoolean(false);
        referee.registerListener(new RefereeListener() {
            @Override
            public void onStart() {
                log.info("Game started");
                running.set(true);
            }

            @Override
            public void onStop() {
                log.info("Game stopped");
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

    @SneakyThrows
    public void loop(ColoredImage rgbImage) {
        ColoredImage hsvImage = imageProcessorService.convertRgbToHsv(rgbImage);
        GrayscaleImage ballMap = imageProcessorService.generateBallCertaintyMap(hsvImage);
        GrayscaleImage blueMap = imageProcessorService.generateBlueCertaintyMap(hsvImage);
        GrayscaleImage yellowMap = imageProcessorService.generateYellowCertaintyMap(hsvImage);
        Collection<Blob> balls = ballDetector.findBlobs(ballMap);

        Collection<GameObject> visibleObjects = new ArrayList<>();
        balls.stream()
             .map((blob) -> new GameObject(blob, 0, 0, GameObjectType.BALL))
             .collect(Collectors.toCollection(() -> visibleObjects));
        MainboardState mainboardState = new MainboardState(mainboard.getDribblerFilled(),
                                                           mainboard.getCoilgunCharged());

        behaviour.stateUpdate(new RobotState(visibleObjects, mainboardState));
        if(isDebug()) {
            debugWindow.setImage(ImagePanel.ORIGINAL, rgbImage, "Original");
            debugWindow.setImage(ImagePanel.MUTATED1, ballMap, "Ball map");
            debugWindow.setImage(ImagePanel.MUTATED3, blueMap, "Blue map");
            debugWindow.setImage(ImagePanel.MUTATED4, yellowMap, "Yellow map");
            debugWindow.setImage(ImagePanel.MUTATED5, imageProcessorService.convertHsvToRgb(hsvImage),
                                 "RGB -> HSV -> RGB");
        }
    }

}
