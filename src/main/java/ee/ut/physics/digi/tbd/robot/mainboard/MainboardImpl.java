package ee.ut.physics.digi.tbd.robot.mainboard;


import com.fazecast.jSerialComm.SerialPort;
import ee.ut.physics.digi.tbd.robot.Settings;
import ee.ut.physics.digi.tbd.robot.mainboard.command.MainboardCommand;
import ee.ut.physics.digi.tbd.robot.mainboard.command.MotorSpeedCommand;
import ee.ut.physics.digi.tbd.robot.mainboard.command.MotorStopCommand;
import ee.ut.physics.digi.tbd.robot.util.SerialUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
public class MainboardImpl implements Mainboard {

    private static final int MAX_SPEED = 255;
    private final SerialPort serialPort;
    private final Thread readerThread;


    public MainboardImpl() {
        serialPort = SerialUtil.openPort(Settings.getInstance().getMainboardPortName());
        serialPort.setComPortParameters(19200, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 100);
        readerThread = new Thread(() -> {
                while(true) {
                    try {
                        //noinspection ResultOfMethodCallIgnored
                        serialPort.getInputStream().read();
                    } catch(IOException e) {
                        break;
                    }
                }
        }, "Mainboard reader thread");
        readerThread.start();
    }


    @Override
    @SneakyThrows
    public void sendCommand(MainboardCommand command) {
        try {
            String commandString = getCommandString(command);
            log.debug("Writing to serial port: \"" + commandString + "\\n\"");
            serialPort.getOutputStream().write((commandString + "\n").getBytes());
            serialPort.getOutputStream().flush();
        } catch(IOException e) {
            throw new IllegalStateException("Failed to write to mainboard", e);
        }
    }

    @Override
    public void sendCommandsBatch(MainboardCommand... commands) {
        String batchCommand = Arrays.stream(commands)
                                    .map(this::getCommandString)
                                    .collect(Collectors.joining("\n")) + "\n";

        log.debug("Writing to serial port: \"" + batchCommand.replace("\n", "\\n") + "\"");
        try {
            serialPort.getOutputStream().write((batchCommand).getBytes());
            serialPort.getOutputStream().flush();
            log.error(batchCommand.replace("\n", "\\n"));
        } catch(IOException e) {
            throw new IllegalStateException("Failed to write to mainboard", e);
        }
    }

    private String getCommandString(MainboardCommand command) {
        if(command instanceof MotorSpeedCommand) {
            MotorSpeedCommand speedCommand = (MotorSpeedCommand) command;
            return String.format("wl%d%d", getMotorIdentifier(speedCommand.getMotor()),
                                 getTransformedSpeed(speedCommand));
        }
        if(command instanceof MotorStopCommand) {
            MotorStopCommand stopCommand = (MotorStopCommand) command;

            return String.format("wl%d0", getMotorIdentifier(stopCommand.getMotor()));
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
                    case FORWARD: return false;
                    case BACK: return true;
                }
                break;
            case RIGHT:
                switch(direction) {
                    case FORWARD: return true;
                    case BACK: return false;
                }
                break;
        }
        throw new IllegalArgumentException("Motor \"" + motor.name() + "\" cannot move in direction + \"" +
                                           direction.name() + "\"");
    }

    private int getMotorIdentifier(Motor motor) {
        switch(motor) {
            case LEFT: return 0;
            case BACK: return 1;
            case RIGHT: return 2;
        }
        throw new IllegalArgumentException("Identifier for motor \"" + motor.name() + "\" not defined");
    }

}
