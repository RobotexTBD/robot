package ee.ut.physics.digi.tbd.robot.kernel;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLMemory;
import ee.ut.physics.digi.tbd.robot.matrix.image.BinaryImage;
import ee.ut.physics.digi.tbd.robot.matrix.image.GrayscaleImage;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.IntBuffer;

@Slf4j
public class ThresholderKernel extends Kernel {

    private final int absoluteSize;
    private final int localWorkgroupSize;
    private final int globalWorkgroupSize;

    private final CLBuffer<IntBuffer> inputBuffer;
    private final CLBuffer<IntBuffer> outputBuffer;

    public ThresholderKernel(int width, int height) throws IOException {
        super("kernel/threshold.cl", "thresholdKernel");
        absoluteSize = width * height;
        localWorkgroupSize = Math.min(device.getMaxWorkGroupSize(), absoluteSize);
        globalWorkgroupSize = calculateGlobalWorkgroupSize(absoluteSize, localWorkgroupSize);
        inputBuffer = context.createBuffer(Buffers.newDirectIntBuffer(absoluteSize), CLMemory.Mem.READ_ONLY);
        outputBuffer = context.createBuffer(Buffers.newDirectIntBuffer(absoluteSize), CLMemory.Mem.READ_WRITE);
    }

    public BinaryImage threshold(GrayscaleImage image, float min, float max, int maxValue) {
        return threshold(image, (int) (maxValue * min), (int) (maxValue * max));
    }

    public BinaryImage threshold(GrayscaleImage image, int min, int max) {
        inputBuffer.getBuffer().put(image.getData()).rewind();
        kernel.putArgs(inputBuffer, outputBuffer)
              .putArg(min)
              .putArg(max)
              .putArg(image.getElementCount()).rewind();
        commandQueue.putWriteBuffer(inputBuffer, false)
                    .put1DRangeKernel(kernel, 0, globalWorkgroupSize, localWorkgroupSize)
                    .putReadBuffer(outputBuffer, true);
        BinaryImage thresholded = new BinaryImage(image.getWidth(), image.getHeight());
        for(int i = 0; i < absoluteSize; i++) {
            thresholded.getData()[i] = outputBuffer.getBuffer().get() > 0;
        }
        outputBuffer.getBuffer().rewind();
        return thresholded;
    }



}
