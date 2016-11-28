package ee.ut.physics.digi.tbd.robot.context;

import lombok.Getter;

public class RobotContextHolder {

    @Getter
    public static final RobotContext context = new RobotContext(new RobotConfig());

}
