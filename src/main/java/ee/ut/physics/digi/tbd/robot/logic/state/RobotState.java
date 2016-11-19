package ee.ut.physics.digi.tbd.robot.logic.state;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collection;

@AllArgsConstructor
@Getter
public class RobotState {

    Collection<GameObject> visibleObjects;
    MainboardState mainboardState;

}
