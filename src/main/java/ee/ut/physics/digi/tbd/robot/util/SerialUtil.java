package ee.ut.physics.digi.tbd.robot.util;

import com.fazecast.jSerialComm.SerialPort;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

@Slf4j
public class SerialUtil {

    private SerialUtil() {}

    public static SerialPort openPort(String nameFragment) {
        SerialPort serialPort = Arrays.stream(SerialPort.getCommPorts())
                                       .filter(port -> port.getDescriptivePortName().contains(nameFragment))
                                       .findAny()
                                       .orElseThrow(() -> new IllegalArgumentException("Cannot find serial port \"" +
                                                                                       nameFragment + "\""));
        if(!serialPort.openPort()) {
            throw new IllegalStateException("Unable to open port \"" + serialPort.getDescriptivePortName() + "\"");
        }
        log.debug("Serial port \"" + serialPort.getDescriptivePortName() + "\" opened");
        return serialPort;
    }

}
    