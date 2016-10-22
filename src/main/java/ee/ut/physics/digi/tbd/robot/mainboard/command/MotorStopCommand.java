package ee.ut.physics.digi.tbd.robot.mainboard.command;

import ee.ut.physics.digi.tbd.robot.mainboard.Motor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@ToString
public class MotorStopCommand implements MainboardCommand {

    private final Motor motor;

}
