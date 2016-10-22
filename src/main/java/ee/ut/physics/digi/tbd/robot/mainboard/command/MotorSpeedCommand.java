package ee.ut.physics.digi.tbd.robot.mainboard.command;

import ee.ut.physics.digi.tbd.robot.mainboard.Direction;
import ee.ut.physics.digi.tbd.robot.mainboard.Motor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@ToString
public class MotorSpeedCommand implements MainboardCommand {

    private final Motor motor;
    private final float speed;
    private final Direction direction;

}
