package ee.ut.physics.digi.tbd.robot.logic;

import ee.ut.physics.digi.tbd.robot.logic.state.RobotState;
import ee.ut.physics.digi.tbd.robot.mainboard.Mainboard;

public abstract class RobotBehaviour {

    public abstract void stateUpdate(RobotState state);

    public abstract void setMainboard(Mainboard mainboard);

}
