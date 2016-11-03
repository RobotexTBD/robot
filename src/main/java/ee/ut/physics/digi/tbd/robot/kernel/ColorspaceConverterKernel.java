package ee.ut.physics.digi.tbd.robot.kernel;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLMemory;
import ee.ut.physics.digi.tbd.robot.matrix.image.ColoredImage;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.IntBuffer;

@Slf4j
public class ColorspaceConverterKernel extends Kernel {

    private final int absoluteSize;
    private final int localWorkgroupSize;
    private final int globalWorkgroupSize;

    private final CLBuffer<IntBuffer> inputBuffer;
    private final CLBuffer<IntBuffer> outputBuffer;

    public ColorspaceConverterKernel(int width, int height, String kernelFileName, String kernelName) throws IOException {
        super(kernelFileName, kernelName);
        absoluteSize = width * height;
        localWorkgroupSize = Math.min(device.getMaxWorkGroupSize(), absoluteSize);
        globalWorkgroupSize = calculateGlobalWorkgroupSize(absoluteSize, localWorkgroupSize);
        inputBuffer = context.createBuffer(Buffers.newDirectIntBuffer(globalWorkgroupSize), CLMemory.Mem.READ_ONLY);
        outputBuffer = context.createBuffer(Buffers.newDirectIntBuffer(globalWorkgroupSize), CLMemory.Mem.READ_WRITE);
    }

    public ColoredImage convert(ColoredImage inputImage) {
        inputBuffer.getBuffer().put(inputImage.getData()).rewind();
        kernel.putArgs(inputBuffer, outputBuffer).putArg(inputImage.getElementCount()).rewind();
        commandQueue.putWriteBuffer(inputBuffer, false)
                    .put1DRangeKernel(kernel, 0, globalWorkgroupSize, localWorkgroupSize)
                    .putReadBuffer(outputBuffer, true);
        ColoredImage outputImage = new ColoredImage(inputImage.getWidth(), inputImage.getHeight());
        outputBuffer.getBuffer().get(outputImage.getData());
        outputBuffer.getBuffer().rewind();
        return outputImage;
    }

}
