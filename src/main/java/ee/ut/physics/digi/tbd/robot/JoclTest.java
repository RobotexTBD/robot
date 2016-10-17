package ee.ut.physics.digi.tbd.robot;

import com.jogamp.opencl.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.IntBuffer;

import static java.lang.System.nanoTime;

@Slf4j
public class JoclTest {

    public static void main(String[] args) throws IOException {

        // set up (uses default CLPlatform and creates context for all devices)
        CLContext context = CLContext.create();
        log.debug("Created context " + context);

        // always make sure to release the context under all circumstances
        // not needed for this particular sample but recommented
        try {

            // select fastest device
            CLDevice device = context.getMaxFlopsDevice();
            log.debug("Using device " + device);

            // create command queue on device.
            CLCommandQueue queue = device.createCommandQueue();

            int elementCount = 16;                                  // Length of arrays to process
            int localWorkSize = Math.min(device.getMaxWorkGroupSize(), 16);  // Local work size dimensions
            int globalWorkSize = roundUp(localWorkSize, elementCount);   // rounded up to the nearest multiple of the localWorkSize

            // load sources, create and build program
            CLProgram program = context.createProgram(JoclTest.class.getClassLoader().getResourceAsStream("ballCertaintyMap.cl")).build();

            // A, B are input buffers, C is for the result
            CLBuffer<IntBuffer> inputBuffer = context.createIntBuffer(globalWorkSize, CLMemory.Mem.READ_ONLY);
            CLBuffer<IntBuffer> outputBuffer = context.createIntBuffer(globalWorkSize, CLMemory.Mem.WRITE_ONLY);

            // fill input buffers with random numbers
            // (just to have test data; seed is fixed -> results will not change between runs).
            fillBuffer(inputBuffer.getBuffer());

            // get a reference to the kernel function with the name 'VectorAdd'
            // and map the buffers to its input parameters.
            CLKernel kernel = program.createCLKernel("ballCertaintyKernel");
            kernel.putArgs(inputBuffer, outputBuffer).putArg(elementCount);

            // asynchronous write of data to GPU device,
            // followed by blocking read to get the computed results back.
            long time = nanoTime();
            queue.putWriteBuffer(inputBuffer, false)
                 .put1DRangeKernel(kernel, 0, globalWorkSize, localWorkSize)
                 .putReadBuffer(outputBuffer, true);
            time = nanoTime() - time;

            StringBuilder result = new StringBuilder();
            IntBuffer resultBuffer = outputBuffer.getBuffer();
            result.append(String.valueOf(resultBuffer.get()));
            while(resultBuffer.hasRemaining()) {
                result.append(", ").append(Integer.toHexString(resultBuffer.get()));
            }
            System.out.println(result);

        }finally{
            // cleanup all resources associated with this context.
            context.release();
        }

    }

    private static void fillBuffer(IntBuffer buffer) {
        buffer.put(0x00000000);
        buffer.put(0x00FF0000);
        buffer.put(0x00FFFF00);
        buffer.put(0x0000FF00);
        buffer.put(0x0000FFFF);
        buffer.put(0x000000FF);
        buffer.put(0x00FF00FF);
        while(buffer.remaining() != 0)
            buffer.put(0x0044B2);
        buffer.rewind();
    }

    private static int roundUp(int groupSize, int globalSize) {
        int r = globalSize % groupSize;
        if (r == 0) {
            return globalSize;
        } else {
            return globalSize + groupSize - r;
        }
    }


}
