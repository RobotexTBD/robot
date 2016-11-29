package ee.ut.physics.digi.tbd.robot;

import com.github.sarxos.webcam.Webcam;
import ee.ut.physics.digi.tbd.robot.context.RobotContext;
import ee.ut.physics.digi.tbd.robot.context.RobotContextHolder;
import ee.ut.physics.digi.tbd.robot.debug.DebugWindow;
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
import ee.ut.physics.digi.tbd.robot.mainboard.Motor;
import ee.ut.physics.digi.tbd.robot.mainboard.command.MotorStopCommand;
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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
public class Robot implements Runnable {

    private final RobotContext context = RobotContextHolder.getContext();
    private final CameraReader cameraReader;
    private final BallDetector ballDetector;
    private final GoalDetector goalDetector;
    private final ImageProcessorService imageProcessorService = context.getImageProcessorService();
    private final Mainboard mainboard;
    private final Referee referee;
    private final Settings settings;

    private DebugWindow debugWindow;
    private RobotBehaviour behaviour;

    public Robot() throws IOException {
        Webcam camera = CameraUtil.openCamera(Settings.getInstance().getWebcamName(),
                                              RobotContext.CAMERA_WIDTH, RobotContext.CAMERA_HEIGHT);
        settings = Settings.getInstance();
        if(isDebug()) {
            debugWindow = DebugWindow.getInstance();
        }
        cameraReader = new CameraReader(camera);
        ballDetector = new BallDetector();
        goalDetector = new GoalDetector();
        mainboard = context.getMainboard();
        referee = context.getReferee();
    }


    public static void main(String[] args) throws IOException {
        new Robot().run();
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
                behaviour = RobotBehaviourFactory.getInstance().getBehaviour();
                behaviour.setMainboard(mainboard);
                running.set(true);
            }

            @Override
            public void onStop() {
                log.info("Game stopped");
                running.set(false);
                mainboard.sendCommandsBatch(new MotorStopCommand(Motor.RIGHT),
                                            new MotorStopCommand(Motor.LEFT),
                                            new MotorStopCommand(Motor.BACK));
                behaviour = null;
            }
        });
        while(true) {
            ColoredImage rgbImage = cameraReader.readRgbImage();
            long startTime = System.currentTimeMillis();
            loop(rgbImage);
            log.trace("Loop took " + (System.currentTimeMillis() - startTime) + " ms");
            if(isDebug()) {
                Platform.runLater(debugWindow::renderImages);
            }
        }
    }

    @SneakyThrows
    public void loop(ColoredImage rgbImage) {
        ColoredImage balancedRgbImage = imageProcessorService.whiteBalance(rgbImage);
        ColoredImage hsvImage = imageProcessorService.convertRgbToHsv(balancedRgbImage);
        GrayscaleImage ballMap = imageProcessorService.generateBallCertaintyMap(hsvImage);
        GrayscaleImage blueMap = imageProcessorService.generateBlueCertaintyMap(hsvImage);
        GrayscaleImage yellowMap = imageProcessorService.generateYellowCertaintyMap(hsvImage);
        Collection<Blob> balls = ballDetector.findBlobs(ballMap);

        Collection<GameObject> visibleObjects = new ArrayList<>();
        balls.stream()
             .map((blob) -> new GameObject(blob, 0, 0, GameObjectType.BALL))
             .collect(Collectors.toCollection(() -> visibleObjects));

        GameObject goal;
        GrayscaleImage goalMap = settings.getTargetGoal().equalsIgnoreCase("yellow") ? yellowMap : blueMap;
        goal = Optional.ofNullable(goalDetector.findGoal(goalMap))
                       .map(blob -> new GameObject(blob, 0, 0, GameObjectType.TARGET_GOAL))
                       .orElse(null);
        if(goal != null) {
            visibleObjects.add(goal);
        }

        MainboardState mainboardState = new MainboardState(mainboard.getDribblerFilled(),
                                                           mainboard.getCoilgunCharged());
        if(behaviour != null) {
            behaviour.stateUpdate(new RobotState(visibleObjects, mainboardState));
        }
        if(isDebug()) {
            debugWindow.setImage(rgbImage, "Original");
            debugWindow.setImage(ballMap, "Ball map");
            debugWindow.setImage(balancedRgbImage, "Balanced");
            debugWindow.setImage(blueMap, "Blue map");
            debugWindow.setImage(yellowMap, "Yellow map");
            debugWindow.setImage(imageProcessorService.convertHsvToRgb(hsvImage), "RGB -> HSV -> RGB");
        }
    }

}
