package ee.ut.physics.digi.tbd.robot.kernel;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLMemory;
import ee.ut.physics.digi.tbd.robot.model.ColoredImage;
import ee.ut.physics.digi.tbd.robot.model.GrayscaleImage;

import java.io.IOException;
import java.nio.IntBuffer;

public class BallDetectorKernel extends Kernel {

    private final int absoluteSize;
    private final int localWorkgroupSize;
    private final int globalWorkgroupSize;

    private final CLBuffer<IntBuffer> inputBuffer;
    private final CLBuffer<IntBuffer> outputBuffer;

    public BallDetectorKernel(int width, int height) throws IOException {
        super("ballCertaintyMap.cl", "ballCertaintyKernel");
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
