package ee.ut.physics.digi.tbd.robot.image.processing.kernel;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLMemory;
import ee.ut.physics.digi.tbd.robot.image.ColoredImage;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.IntBuffer;

@Slf4j
public class WhiteBalanceKernel extends Kernel {

    private final int white = 0xFFFE96;
    private final int black = 0x060800;

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
              .putArg(white).putArg(black).putArg(inputImage.getElementCount()).rewind();
        commandQueue.putWriteBuffer(inputBuffer, false)
                    .put1DRangeKernel(kernel, 0, globalWorkgroupSize, localWorkgroupSize)
                    .putReadBuffer(outputBuffer, true);
        ColoredImage outputImage = new ColoredImage(inputImage.getWidth(), inputImage.getHeight());
        outputBuffer.getBuffer().get(outputImage.getData());
        outputBuffer.getBuffer().rewind();
        return outputImage;
    }

}
