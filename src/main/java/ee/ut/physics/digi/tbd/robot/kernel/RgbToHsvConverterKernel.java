package ee.ut.physics.digi.tbd.robot.kernel;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLMemory;
import ee.ut.physics.digi.tbd.robot.matrix.image.ColoredImage;

import java.io.IOException;
import java.nio.IntBuffer;

public class RgbToHsvConverterKernel extends Kernel {

    private final int absoluteSize;
    private final int localWorkgroupSize;
    private final int globalWorkgroupSize;

    private final CLBuffer<IntBuffer> inputBuffer;
    private final CLBuffer<IntBuffer> outputBuffer;

    public RgbToHsvConverterKernel(int width, int height) throws IOException {
        super("rgbToHsv.cl", "rgbToHsvKernel");
        absoluteSize = width * height;
        localWorkgroupSize = Math.min(device.getMaxWorkGroupSize(), absoluteSize);
        globalWorkgroupSize = calculateGlobalWorkgroupSize(absoluteSize, localWorkgroupSize);
        inputBuffer = context.createBuffer(Buffers.newDirectIntBuffer(globalWorkgroupSize), CLMemory.Mem.READ_ONLY);
        outputBuffer = context.createBuffer(Buffers.newDirectIntBuffer(globalWorkgroupSize), CLMemory.Mem.READ_WRITE);
    }

    public ColoredImage convert(ColoredImage rgbImage) {
        inputBuffer.getBuffer().put(rgbImage.getData()).rewind();
        kernel.putArgs(inputBuffer, outputBuffer).putArg(rgbImage.getElementCount()).rewind();
        commandQueue.putWriteBuffer(inputBuffer, false)
                    .put1DRangeKernel(kernel, 0, globalWorkgroupSize, localWorkgroupSize)
                    .putReadBuffer(outputBuffer, true);
        ColoredImage hsvImage = new ColoredImage(rgbImage.getWidth(), rgbImage.getHeight());
        outputBuffer.getBuffer().get(hsvImage.getData());
        outputBuffer.getBuffer().rewind();
        return hsvImage;
    }

}
