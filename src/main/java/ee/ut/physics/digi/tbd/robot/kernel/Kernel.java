package ee.ut.physics.digi.tbd.robot.kernel;

import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLKernel;

import java.io.IOException;
import java.io.InputStream;

public abstract class Kernel {

    protected final CLCommandQueue commandQueue;
    protected final CLContext context;
    protected final CLDevice device;
    protected final CLKernel kernel;

    public Kernel(String kernelFileName, String kernelName) throws IOException {
        context = CLContext.create();
        device = context.getMaxFlopsDevice();
        commandQueue = device.createCommandQueue();
        kernel = context.createProgram(getInputStream("kernel/" + kernelFileName))
                        .build()
                        .createCLKernel(kernelName);
    }

    protected static InputStream getInputStream(String resourceFileName) {
        return Kernel.class.getClassLoader().getResourceAsStream(resourceFileName);
    }

    protected static int calculateGlobalWorkgroupSize(int absoluteSize, int localWorkgroupSize) {
        int r = absoluteSize % localWorkgroupSize;
        if (r == 0) {
            return absoluteSize;
        } else {
            return absoluteSize + localWorkgroupSize - r;
        }
    }

}
