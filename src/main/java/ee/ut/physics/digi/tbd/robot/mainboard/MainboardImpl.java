package ee.ut.physics.digi.tbd.robot.mainboard;


import com.fazecast.jSerialComm.SerialPort;
import ee.ut.physics.digi.tbd.robot.mainboard.command.MainboardCommand;
import ee.ut.physics.digi.tbd.robot.mainboard.command.MotorSpeedCommand;
import ee.ut.physics.digi.tbd.robot.mainboard.command.MotorStopCommand;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Arrays;

@Slf4j
public class MainboardImpl implements Mainboard {

    private static final String portName = "COM3";
    private static final int MAX_SPEED = 255;
    private final SerialPort serialPort;

    public MainboardImpl() {
        serialPort = Arrays.stream(SerialPort.getCommPorts())
                           .filter(port -> port.getDescriptivePortName().contains(portName))
                           .findFirst()
                           .orElseThrow(() -> new IllegalStateException("Mainboard not connected or port not found"));
        serialPort.setComPortParameters(19200, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 0, 0);
        if(!serialPort.openPort()) {
            throw new IllegalStateException("Unable to open mainboard port");
        }
        log.info("Serial port opened");
    }


    @Override
    public void sendCommand(MainboardCommand command) {
        try {
            String commandString = getCommandString(command);
            log.debug("Writing to serial port: \"" + commandString + "\"");
            serialPort.getOutputStream().write((commandString + "\r\n").getBytes());
            serialPort.getOutputStream().flush();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private String getCommandString(MainboardCommand command) {
        if(command instanceof MotorSpeedCommand) {
            MotorSpeedCommand speedCommand = (MotorSpeedCommand) command;
            return String.format("wl%d%d\0", getMotorIdentifier(speedCommand.getMotor()),
                                 getTransformedSpeed(speedCommand));
        }
        if(command instanceof MotorStopCommand) {
            MotorStopCommand stopCommand = (MotorStopCommand) command;

            return String.format("wl%d0\0", getMotorIdentifier(stopCommand.getMotor()));
        }
        throw new IllegalArgumentException("Handling of command type " + command.getClass().getSimpleName() +
                                           " not implemented");
    }

    private int getTransformedSpeed(MotorSpeedCommand command) {
        int transformedSpeed = (int) (command.getSpeed() * MAX_SPEED);
        return isSpeedNegative(command.getMotor(), command.getDirection()) ? -transformedSpeed : transformedSpeed;
    }

    private boolean isSpeedNegative(Motor motor, Direction direction) {
        switch(motor) {
            case BACK:
                switch(direction) {
                    case LEFT: return true;
                    case RIGHT: return false;
                }
                break;
            case LEFT:
                switch(direction) {
                    case FORWARD: return true;
                    case BACK: return false;
                }
                break;
            case RIGHT:
                switch(direction) {
                    case FORWARD: return false;
                    case BACK: return true;
                }
                break;
        }
        throw new IllegalArgumentException("Motor \"" + motor.name() + "\" cannot move in direction + \"" +
                                           direction.name() + "\"");
    }

    private int getMotorIdentifier(Motor motor) {
        switch(motor) {
            case BACK: return 0;
            case LEFT: return 1;
            case RIGHT: return 2;
        }
        throw new IllegalArgumentException("Identifier for motor \"" + motor.name() + "\" not defined");
    }

}
