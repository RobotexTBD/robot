package ee.ut.physics.digi.tbd.robot.context;

import ee.ut.physics.digi.tbd.robot.debug.ConfigManager;
import ee.ut.physics.digi.tbd.robot.image.processing.ImageProcessorService;
import ee.ut.physics.digi.tbd.robot.mainboard.Mainboard;
import ee.ut.physics.digi.tbd.robot.referee.Referee;
import lombok.Getter;

public final class RobotContext {

    public static final boolean DEBUG = true;

    public static final int CAMERA_WIDTH = 640;
    public static final int CAMERA_HEIGHT = 480;

    private final RobotConfig robotConfig;

    @Getter
    private final ConfigManager configManager;

    @Getter
    private final ImageProcessorService imageProcessorService;

    public RobotContext(RobotConfig robotConfig) {
        this.robotConfig = robotConfig;
        configManager = new ConfigManager();
        imageProcessorService = new ImageProcessorService(CAMERA_WIDTH, CAMERA_HEIGHT);
        configManager.registerConfigurableComponent(robotConfig, "robot");
        imageProcessorService.getKernels()
                             .forEach((name, kernel) -> configManager.registerConfigurableComponent(kernel, name));
    }

    public Mainboard getMainboard() {
        return robotConfig.getMainboardProxy();
    }

    public Referee getReferee() {
        return robotConfig.getRefereeProxy();
    }

    public float getSpeed() {
        return robotConfig.getSpeed();
    }
}
