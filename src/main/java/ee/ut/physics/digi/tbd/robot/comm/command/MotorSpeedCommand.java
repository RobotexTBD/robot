package ee.ut.physics.digi.tbd.robot.comm.command;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class MotorSpeedCommand implements MainboardCommand {

    private float speed;

}
