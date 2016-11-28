package ee.ut.physics.digi.tbd.robot.image.processing.kernel;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLMemory;
import ee.ut.physics.digi.tbd.robot.debug.Configurable;
import ee.ut.physics.digi.tbd.robot.image.ColoredImage;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.IntBuffer;

@Slf4j
public class WhiteBalanceKernel extends Kernel {

    @Configurable(value = "White red", maxInt = 255)
    @Getter @Setter
    private int whiteRed = 0;

    @Configurable(value = "White green", maxInt = 255)
    @Getter @Setter
    private int whiteGreen = 0;

    @Configurable(value = "White blue", maxInt = 255)
    @Getter @Setter
    private int whiteBlue = 0;

    @Configurable(value = "Black red", maxInt = 255)
    @Getter @Setter
    private int blackRed = 0;

    @Configurable(value = "Black green", maxInt = 255)
    @Getter @Setter
    private int blackGreen = 0;

    @Configurable(value = "Black blue", maxInt = 255)
    @Getter @Setter
    private int blackBlue = 0;

    private final int absoluteSize;
    private final int localWorkgroupSize;
    private final int globalWorkgroupSize;

    private final CLBuffer<IntBuffer> inputBuffer;
    private final CLBuffer<IntBuffer> outputBuffer;

    public WhiteBalanceKernel(int width, int height) throws IOException {
        super("whiteBalanceKernel.cl", "whiteBalanceKernel");
        absoluteSize = width * height;
        localWorkgroupSize = Math.min(device.getMaxWorkGroupSize(), absoluteSize);
        globalWorkgroupSize = calculateGlobalWorkgroupSize(absoluteSize, localWorkgroupSize);
        inputBuffer = context.createBuffer(Buffers.newDirectIntBuffer(globalWorkgroupSize), CLMemory.Mem.READ_ONLY);
        outputBuffer = context.createBuffer(Buffers.newDirectIntBuffer(globalWorkgroupSize), CLMemory.Mem.READ_WRITE);
    }

    public ColoredImage balance(ColoredImage inputImage) {
        inputBuffer.getBuffer().put(inputImage.getData()).rewind();
        kernel.putArgs(inputBuffer, outputBuffer)
              .putArg(getWhite()).putArg(getBlack()).putArg(inputImage.getElementCount()).rewind();
        commandQueue.putWriteBuffer(inputBuffer, false)
                    .put1DRangeKernel(kernel, 0, globalWorkgroupSize, localWorkgroupSize)
                    .putReadBuffer(outputBuffer, true);
        ColoredImage outputImage = new ColoredImage(inputImage.getWidth(), inputImage.getHeight());
        outputBuffer.getBuffer().get(outputImage.getData());
        outputBuffer.getBuffer().rewind();
        return outputImage;
    }

    public int getWhite() {
        return (whiteRed << 16) | (whiteGreen << 8) | whiteBlue;
    }

    public int getBlack() {
        return (blackRed << 16) | (blackGreen << 8) | blackBlue;
    }

}
