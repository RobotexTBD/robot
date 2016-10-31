package ee.ut.physics.digi.tbd.robot.referee;

import com.fazecast.jSerialComm.SerialPort;
import ee.ut.physics.digi.tbd.robot.Settings;
import ee.ut.physics.digi.tbd.robot.util.SerialUtil;
import ee.ut.physics.digi.tbd.robot.util.StringUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class RefereeImpl implements Referee {

    private final char fieldId;
    private final char robotId;
    private final SerialPort serialPort;
    private final List<RefereeListener> listeners = new ArrayList<>();
    private final Thread readerThread;

    public RefereeImpl() {
        Settings settings = Settings.getInstance();
        serialPort = SerialUtil.openPort(settings.getRadioPortName());
        fieldId = settings.getFieldId();
        robotId = settings.getRobotId();
        serialPort.setComPortParameters(19200, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 100);
        readerThread = new Thread(this.new RefereeReader(), "Referee reader thread");
        readerThread.start();
    }

    @Override
    public void registerListener(RefereeListener listener) {
        listeners.add(listener);
    }

    @Override
    public void unregisterListener(RefereeListener listener) {
        listeners.remove(listener);
    }

    private class RefereeReader implements Runnable {

        private static final int PACKET_SIZE = 12;
        private static final char COMMAND_MARKER = 'a';
        public static final char ROBOT_ID_WILDCARD = 'X';
        public static final char PADDING_CHAR = '-';

        public void handleData(char[] data) {
            char marker = data[0];
            char eventFieldId = data[1];
            char eventRobotId = data[2];
            if(marker != COMMAND_MARKER || eventFieldId != fieldId ||
               (eventRobotId != ROBOT_ID_WILDCARD && eventRobotId != robotId)) {
                return;
            }
            log.debug("Got referee command: \"" + new String(data) + "\"");
            if(eventRobotId == robotId) {
                sendAck();
            }
            handleCommand(new String(Arrays.copyOfRange(data, 3, PACKET_SIZE)));
        }

        private void handleCommand(String command) {
            if(command.startsWith("START")) {
                listeners.forEach(RefereeListener::onStart);
            } else if(command.startsWith("STOP")) {
                listeners.forEach(RefereeListener::onStop);
            }
        }

        private void sendAck() {
            String header = new String(new char[] { COMMAND_MARKER, fieldId, robotId });
            String ackString = StringUtil.padRight(header + "ACK", PADDING_CHAR, PACKET_SIZE);
            try {
                log.debug("Sending \"" + ackString + "\" to referee");
                serialPort.getOutputStream().write(ackString.getBytes());
                serialPort.getOutputStream().flush();
            } catch(IOException e) {
                log.debug("Unable to send data to referee", e);
            }
        }

        @SuppressWarnings("InfiniteLoopStatement")
        @Override
        @SneakyThrows
        public void run() {
            char[] buffer = new char[PACKET_SIZE * 2];
            int bufferOffset = 0;
            while(true) {
                char element = (char) serialPort.getInputStream().read();
                buffer[bufferOffset] = element;
                buffer[bufferOffset + PACKET_SIZE] = element;
                char[] bufferCopy = Arrays.copyOfRange(buffer,bufferOffset, bufferOffset + PACKET_SIZE);
                handleData(bufferCopy);
                bufferOffset++;
                bufferOffset %= PACKET_SIZE;
            }
        }

    }
}
