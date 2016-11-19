package ee.ut.physics.digi.tbd.robot.logic;

import ee.ut.physics.digi.tbd.robot.logic.main.MainRobotBehaviour;

public final class RobotBehaviourFactory {

    private static RobotBehaviourFactory instance;

    private RobotBehaviourFactory() {}

    public RobotBehaviour getBehaviour() {
        return new MainRobotBehaviour();
    }

    public static RobotBehaviourFactory getInstance() {
        if(instance == null) {
            instance = new RobotBehaviourFactory();
        }
        return instance;
    }

}
