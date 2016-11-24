package ee.ut.physics.digi.tbd.robot.mainboard;


import com.fazecast.jSerialComm.SerialPort;
import ee.ut.physics.digi.tbd.robot.Settings;
import ee.ut.physics.digi.tbd.robot.mainboard.command.*;
import ee.ut.physics.digi.tbd.robot.util.SerialUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
public class MainboardImpl implements Mainboard {

    private static final int MAX_SPEED = 255;
    private final SerialPort serialPort;
    private final Thread readerThread;
    private final Thread coilgunChargerThread;
    private final AtomicBoolean coilCharged = new AtomicBoolean(false);
    private final AtomicBoolean dribblerFilled = new AtomicBoolean(false);

    private final Object commandWriterLock = new Object();
    private final Object coilgunChargerThreadLock = new Object();

    private final Settings settings = Settings.getInstance();

    public MainboardImpl() {
        serialPort = SerialUtil.openPort(Settings.getInstance().getMainboardPortName());
        serialPort.setComPortParameters(19200, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 100);
        readerThread = new Thread(this.new EventReader(), "Mainboard reader thread");
        readerThread.start();
        coilgunChargerThread = new Thread(this.new CoilgunCharger(), "Coilgun charger thread");
        coilgunChargerThread.start();
        sendInitCommands();
    }

    @SneakyThrows
    private void sendInitCommands() {
        sendCommand(new DribblerStopCommand());
        sendCommandBytes("t10000\n".getBytes());
    }

    @Override
    @SneakyThrows
    public void sendCommand(MainboardCommand command) {
        try {
            String commandString = getCommandString(command);
            log.debug("Writing to serial port: \"" + commandString + "\\n\"");
            sendCommandBytes((commandString + "\n").getBytes());
        } catch(IOException e) {
            throw new IllegalStateException("Failed to write to mainboard", e);
        }
    }

    private void sendCommandBytes(byte[] bytes) throws IOException {
        synchronized(commandWriterLock) {
            serialPort.getOutputStream().write(bytes);
            serialPort.getOutputStream().flush();
        }
    }

    @Override
    public boolean getDribblerFilled() {
        return dribblerFilled.get();
    }

    @Override
    public boolean getCoilgunCharged() {
        return coilCharged.get();
    }

    @Override
    public void sendCommandsBatch(MainboardCommand... commands) {
        String batchCommand = Arrays.stream(commands)
                                    .map(this::getCommandString)
                                    .collect(Collectors.joining("\n")) + "\n";

        log.debug("Writing to serial port: \"" + batchCommand.replace("\n", "\\n") + "\"");
        try {
            sendCommandBytes(batchCommand.getBytes());
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
        if(command instanceof KickCommand) {
            return "k";
        }
        if(command instanceof DribblerStartCommand) {
            return String.format("d%d", Settings.getInstance().getDribblerRollSpeed());
        }
        if(command instanceof DribblerStopCommand) {
            return String.format("d%d", Settings.getInstance().getDribblerInitSpeed());
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

    private class EventReader implements Runnable {

        private static final int EVENT_READER_BUFFER_SIZE = 32;

        boolean readingEvent = false;
        char[] eventBuffer = new char[EVENT_READER_BUFFER_SIZE];
        int eventBufferOffset = 0;

        @SuppressWarnings("InfiniteLoopStatement")
        @Override
        public void run() {
            while(true) {
                loop();
            }
        }

        private void loop() {
            try {
                char singleChar = (char) serialPort.getInputStream().read();
                if(singleChar == '{') {
                    readingEvent = true;
                } else if(singleChar == '}') {
                    readingEvent = false;
                    if(eventBufferOffset > 0 ) {
                        handleEvent(new String(eventBuffer, 0, eventBufferOffset));
                        eventBufferOffset = 0;
                    }
                } else {
                    eventBuffer[eventBufferOffset] = singleChar;
                    eventBufferOffset++;
                    if(eventBufferOffset >= EVENT_READER_BUFFER_SIZE) {
                        log.warn("Mainboard event reader buffer overflow");
                        readingEvent = false;
                        eventBufferOffset = 0;
                    }
                }
            } catch(IOException e) {
                log.error("Error reading data from mainboard", e);
            }
        }

        private void handleEvent(String event) {
            String[] eventParts = event.split(":");
            if(eventParts.length == 1) {
                handleEvent(eventParts[0].trim(), "");
            } else if(eventParts.length == 2) {
                handleEvent(eventParts[0].trim(), eventParts[1].trim());
            } else {
                log.warn("Got invalid event \"" + event + "\" from mainboard.");
            }
        }

        private void handleEvent(String key, String value) {
            if(key.equalsIgnoreCase("kicked")) {
                coilCharged.set(false);
                synchronized(coilgunChargerThreadLock) {
                    coilgunChargerThreadLock.notify();
                }
            } else if(key.equalsIgnoreCase("dribblerFilled")) {
                dribblerFilled.set(value.equalsIgnoreCase("1") || value.equalsIgnoreCase("true"));
            }
        }

    }

    private class CoilgunCharger implements Runnable {

        @SuppressWarnings("InfiniteLoopStatement")
        @Override
        @SneakyThrows
        public void run() {
            while(true) {
                if(!coilCharged.get()) {
                    sendCommandBytes("c1\n".getBytes());
                    synchronized(coilgunChargerThreadLock) {
                        coilgunChargerThreadLock.wait(settings.getCoilgunChargeInitTime());
                    }
                    continue;
                }
                sendCommandBytes("c0\n".getBytes());
                synchronized(coilgunChargerThreadLock) {
                    coilgunChargerThreadLock.wait(settings.getCoilgunChargeWaitTime());
                }
                if(coilCharged.get()) {
                    continue;
                }
                sendCommandBytes("c1\n".getBytes());
                synchronized(coilgunChargerThreadLock) {
                    coilgunChargerThreadLock.wait(settings.getCoilgunChargeRechargeTime());
                }
            }
        }

    }

}
