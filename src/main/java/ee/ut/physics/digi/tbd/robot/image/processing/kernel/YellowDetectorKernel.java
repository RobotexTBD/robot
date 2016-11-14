package ee.ut.physics.digi.tbd.robot.image.processing.kernel;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class YellowDetectorKernel extends CertaintyMapKernel {

    public YellowDetectorKernel(int width, int height) throws IOException {
        super(width, height, "yellowCertaintyMap.cl", "yellowCertaintyKernel");
    }

}
