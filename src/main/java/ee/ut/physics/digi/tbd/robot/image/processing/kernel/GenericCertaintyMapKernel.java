package ee.ut.physics.digi.tbd.robot.image.processing.kernel;

import ee.ut.physics.digi.tbd.robot.debug.Configurable;
import ee.ut.physics.digi.tbd.robot.image.ColoredImage;
import ee.ut.physics.digi.tbd.robot.image.GrayscaleImage;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class GenericCertaintyMapKernel extends CertaintyMapKernel {

    @Configurable(value = "Minimum hue", maxInt = 180)
    @Getter @Setter
    private int minHue = 0;

    @Configurable(value = "Minimum saturation", maxInt = 255)
    @Getter @Setter
    private int minSaturation = 0;

    @Configurable(value = "Minimum value", maxInt = 255)
    @Getter @Setter
    private int minValue = 0;

    @Configurable(value = "Maximum hue", maxInt = 180)
    @Getter @Setter
    private int maxHue = 0;

    @Configurable(value = "Maximum saturation", maxInt = 255)
    @Getter @Setter
    private int maxSaturation = 0;

    @Configurable(value = "Maximum value", maxInt = 255)
    @Getter @Setter
    private int maxValue = 0;


    public GenericCertaintyMapKernel(int width, int height) throws IOException {
        super(width, height, "genericCertaintyMap.cl", "genericCertaintyMapKernel");
    }

    public GrayscaleImage generateCertaintyMap(ColoredImage hsvImage) {
        inputBuffer.getBuffer().put(hsvImage.getData()).rewind();
        kernel.putArgs(inputBuffer, outputBuffer)
              .putArg(getMinColor()).putArg(getMaxColor()).putArg(hsvImage.getElementCount()).rewind();
        commandQueue.putWriteBuffer(inputBuffer, false)
                    .put1DRangeKernel(kernel, 0, globalWorkgroupSize, localWorkgroupSize)
                    .putReadBuffer(outputBuffer, true);
        GrayscaleImage certaintyMap = new GrayscaleImage(hsvImage.getWidth(), hsvImage.getHeight());
        outputBuffer.getBuffer().get(certaintyMap.getData());
        outputBuffer.getBuffer().rewind();
        return certaintyMap;
    }

    private int getMinColor() {
        return (minHue << 16) | (minSaturation << 8) | minValue;
    }

    private int getMaxColor() {
        return (maxHue << 16) | (maxSaturation << 8) | maxValue;
    }

}
