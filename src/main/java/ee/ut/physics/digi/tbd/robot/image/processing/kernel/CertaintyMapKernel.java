package ee.ut.physics.digi.tbd.robot.image.processing.kernel;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLMemory;
import ee.ut.physics.digi.tbd.robot.image.ColoredImage;
import ee.ut.physics.digi.tbd.robot.image.GrayscaleImage;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.IntBuffer;

@Slf4j
public abstract class CertaintyMapKernel extends Kernel {

    protected final int absoluteSize;
    protected final int localWorkgroupSize;
    protected final int globalWorkgroupSize;

    protected final CLBuffer<IntBuffer> inputBuffer;
    protected final CLBuffer<IntBuffer> outputBuffer;

    public CertaintyMapKernel(int width, int height, String kernelFileName, String kernelName) throws IOException {
        super(kernelFileName, kernelName);
        absoluteSize = width * height;
        localWorkgroupSize = Math.min(device.getMaxWorkGroupSize(), absoluteSize);
        globalWorkgroupSize = calculateGlobalWorkgroupSize(absoluteSize, localWorkgroupSize);
        inputBuffer = context.createBuffer(Buffers.newDirectIntBuffer(absoluteSize), CLMemory.Mem.READ_ONLY);
        outputBuffer = context.createBuffer(Buffers.newDirectIntBuffer(absoluteSize), CLMemory.Mem.READ_WRITE);
    }

    public GrayscaleImage generateCertaintyMap(ColoredImage hsvImage) {
        inputBuffer.getBuffer().put(hsvImage.getData()).rewind();
        kernel.putArgs(inputBuffer, outputBuffer).putArg(hsvImage.getElementCount()).rewind();
        commandQueue.putWriteBuffer(inputBuffer, false)
                    .put1DRangeKernel(kernel, 0, globalWorkgroupSize, localWorkgroupSize)
                    .putReadBuffer(outputBuffer, true);
        GrayscaleImage certaintyMap = new GrayscaleImage(hsvImage.getWidth(), hsvImage.getHeight());
        outputBuffer.getBuffer().get(certaintyMap.getData());
        outputBuffer.getBuffer().rewind();
        return certaintyMap;
    }

}
