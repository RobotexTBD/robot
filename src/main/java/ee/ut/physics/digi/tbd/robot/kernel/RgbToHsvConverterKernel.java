package ee.ut.physics.digi.tbd.robot.kernel;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class RgbToHsvConverterKernel extends ColorspaceConverterKernel {

    public RgbToHsvConverterKernel(int width, int height) throws IOException {
        super(width, height, "rgbToHsv.cl", "rgbToHsvKernel");
    }

}
