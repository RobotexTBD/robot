package ee.ut.physics.digi.tbd.robot.util;

import com.fazecast.jSerialComm.SerialPort;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

@Slf4j
public class SerialUtil {

    private SerialUtil() {}

    public static SerialPort openPort(String nameFragment) {
        RuntimeException portNotFoundException = new IllegalArgumentException("Cannot find serial port \"" +
                                                                              nameFragment + "\"");
        if(nameFragment == null || nameFragment.isEmpty()) {
            throw portNotFoundException;
        }
        SerialPort serialPort = Arrays.stream(SerialPort.getCommPorts())
                                       .filter(port -> port.getDescriptivePortName().contains(nameFragment))
                                       .findAny()
                                       .orElseThrow(() -> portNotFoundException);
        if(!serialPort.openPort()) {
            throw new IllegalStateException("Unable to open port \"" + serialPort.getDescriptivePortName() + "\"");
        }
        log.debug("Serial port \"" + serialPort.getDescriptivePortName() + "\" opened");
        return serialPort;
    }

}
    