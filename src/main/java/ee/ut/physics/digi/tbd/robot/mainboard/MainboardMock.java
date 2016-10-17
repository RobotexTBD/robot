package ee.ut.physics.digi.tbd.robot.mainboard;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MainboardMock implements Mainboard {

    @Override
    public void setSpeed(Motor motor, float speed, Direction direction) {
        if(!Float.isFinite(speed) || speed > 1.0f || speed < -1.0f) {
            throw new IllegalArgumentException("Motor speed must be in range -1.0 ... 1.0");
        }
        log.warn("Motor " + motor.name() + " speed set to " + String.format("%.2f", speed * 100) + "%");
    }

}
