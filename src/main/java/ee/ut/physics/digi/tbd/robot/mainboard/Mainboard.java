package ee.ut.physics.digi.tbd.robot.mainboard;

import ee.ut.physics.digi.tbd.robot.mainboard.command.MainboardCommand;

public interface Mainboard {

    void sendCommand(MainboardCommand command);

}
