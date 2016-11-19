package ee.ut.physics.digi.tbd.robot.mainboard;

import ee.ut.physics.digi.tbd.robot.mainboard.command.MainboardCommand;

import java.util.Arrays;

public interface Mainboard {

    void sendCommand(MainboardCommand command);

    boolean getDribblerFilled();

    boolean getCoilgunCharged();

    default void sendCommandsBatch(MainboardCommand... commands) {
        Arrays.stream(commands).forEach(this::sendCommand);
    }

}
