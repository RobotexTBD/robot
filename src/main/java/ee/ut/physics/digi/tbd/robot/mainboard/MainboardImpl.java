package ee.ut.physics.digi.tbd.robot.mainboard;


import com.fazecast.jSerialComm.SerialPort;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Arrays;

@Slf4j
public class MainboardImpl implements Mainboard {

    private static final String portName = "COM3";
    private static final int MAX_SPEED = 255;
    public final SerialPort serialPort;

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
    @SneakyThrows
    public void setSpeed(Motor motor, float speed, Direction direction) {
        try {
            String command = getSetSpeedCommandString(motor, speed, direction);
            log.debug("Writing to serial port: \"" + command + "\"");
            serialPort.getOutputStream().write((command + "\r\n").getBytes());
            serialPort.getOutputStream().flush();
            Thread.sleep(100);
        } catch(IOException e) {
            throw new IllegalStateException("Unable to send speed change command", e);
        }
    }

    private String getSetSpeedCommandString(Motor motor, float speed, Direction direction) {
        return "wl" + getMotorIdentifier(motor) +
               String.valueOf(getTransformedSpeed(motor, speed, direction));
    }

    private int getTransformedSpeed(Motor motor, float speed, Direction direction) {
        if(direction == Direction.NONE) {
            return 0;
        }
        int transformedSpeed = (int) (speed * MAX_SPEED);
        if(isSpeedNegative(motor, direction)) {
            return -transformedSpeed;
        }
        return transformedSpeed;
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
