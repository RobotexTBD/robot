package ee.ut.physics.digi.tbd.robot.debug;

import com.fazecast.jSerialComm.SerialPort;
import com.github.sarxos.webcam.Webcam;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

@Slf4j
public class DevicePrinter {

    public static void main(String[] args) {
        new DevicePrinter().logAvailableDevices();
    }

    public void logAvailableDevices() {
        logAvailableSerialPorts();
        logAvailableWebCams();
    }

    private void logAvailableSerialPorts() {
        log.info("Found " + SerialPort.getCommPorts().length + " serial ports: ");
        Arrays.stream(SerialPort.getCommPorts())
              .map(p -> p.getDescriptivePortName() + "(" + p.getSystemPortName() + ")")
              .forEach(portName -> log.info("\t" + portName));
    }

    private void logAvailableWebCams() {
        log.info("Found " + Webcam.getWebcams().size() + " webcams: ");
        Webcam.getWebcams().stream()
              .map(Webcam::getName)
              .forEach(webcamName -> log.info("\t" + webcamName));
    }

}
