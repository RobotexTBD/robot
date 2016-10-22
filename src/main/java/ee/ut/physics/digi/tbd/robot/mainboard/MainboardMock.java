package ee.ut.physics.digi.tbd.robot.mainboard;

import ee.ut.physics.digi.tbd.robot.mainboard.command.MainboardCommand;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MainboardMock implements Mainboard {

    @Override
    public void sendCommand(MainboardCommand command) {
        log.info(command.toString());
    }

}
