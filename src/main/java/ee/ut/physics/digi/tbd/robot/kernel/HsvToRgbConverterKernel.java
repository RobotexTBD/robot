package ee.ut.physics.digi.tbd.robot.kernel;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class HsvToRgbConverterKernel extends ColorspaceConverterKernel {

    public HsvToRgbConverterKernel(int width, int height) throws IOException {
        super(width, height, "hsvToRgb.cl", "hsvToRgbKernel");
    }

}
