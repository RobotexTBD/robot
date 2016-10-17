package ee.ut.physics.digi.tbd.robot;

import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;
import com.jogamp.opencl.*;
import ee.ut.physics.digi.tbd.robot.util.AngleUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.IntBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BiConsumer;

@Slf4j
public class ImageProcessor {

    private static final float ORANGE_HUE = AngleUtil.toRadians(24.0f);

    private ImageProcessor() {}

    private static float hueDistance(float hue1, float hue2) {
        if(!Float.isFinite(hue1) || !Float.isFinite(hue2)) {
            return 2 * (float) Math.PI;
        }
        return Math.min(Math.abs(hue1 - hue2), Math.min(hue1, hue2) + 2 * (float) Math.PI - hue2);
    }

    public static GrayF32 generateCertaintyMap(Planar<GrayF32> hsv) {

        GrayF32 hue = hsv.getBand(0);
        GrayF32 saturation = hsv.getBand(1);
        GrayF32 value = hsv.getBand(2);
        GrayF32 certainty = saturation.createSameShape();

        CLContext context = CLContext.create();

        try {
            CLDevice device = context.getMaxFlopsDevice();
            CLCommandQueue queue = device.createCommandQueue();

            int elementCount = hue.getWidth() * hue.getHeight();
            int localWorkSize = 64;

            ClassLoader classLoader = ImageProcessor.class.getClassLoader();
            CLProgram program = context.createProgram(classLoader.getResourceAsStream("ballCertaintyMap.cl"))
                                       .build();

            CLBuffer<IntBuffer> inputBuffer = context.createIntBuffer(elementCount, CLMemory.Mem.READ_ONLY);
            CLBuffer<IntBuffer> outputBuffer = context.createIntBuffer(elementCount, CLMemory.Mem.WRITE_ONLY);

            for(int y = 0; y < hue.getHeight(); y++) {
                for(int x = 0; x < hue.getWidth(); x++) {
                    int hueValue = (int) AngleUtil.toDegrees(hue.unsafe_get(x, y)) / 2;
                    int saturationValue = (int) saturation.unsafe_get(x, y);
                    int valueValue = (int) value.unsafe_get(x, y);
                    int packed = hueValue << 16 | saturationValue << 8 | valueValue;
                    inputBuffer.getBuffer().put(packed);
                }
            }

            inputBuffer.getBuffer().rewind();

            CLKernel kernel = program.createCLKernel("ballCertaintyKernel");
            kernel.putArgs(inputBuffer, outputBuffer).putArg(elementCount);

            int minValue = Integer.MAX_VALUE;
            int maxValue = Integer.MIN_VALUE;

            queue.putWriteBuffer(inputBuffer, true)
                 .put1DRangeKernel(kernel, 0, elementCount, localWorkSize)
                 .putReadBuffer(outputBuffer, true);
            for(int y = 0; y < hue.getHeight(); y++) {
                for(int x = 0; x < hue.getWidth(); x++) {
                    int i = outputBuffer.getBuffer().get();
                    minValue = Math.min(minValue, i);
                    maxValue = Math.max(maxValue, i);
                    certainty.unsafe_set(x, y, 2 * (float) i - 255.0f);
                }
            }
            System.out.println(minValue + " " + maxValue);

        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            // cleanup all resources associated with this context.
            context.release();
        }
        return certainty;
    }

    public static Collection<Blob> findBlobsAndFillHoles(GrayU8 binary) {
        long startTime = System.currentTimeMillis();
        int width = binary.getWidth();
        int height = binary.getHeight();
        int stride = width + 2;
        boolean[][] visited = buildVisitedMap(binary);
        Queue<Integer> visitQueue = new ArrayDeque<>();
        for(int x = 1; x <= binary.getWidth(); x++) {
            visitQueue.add(x + stride);
            visitQueue.add(x + stride * height);
        }
        for(int y = 2; y <= binary.getHeight() - 1; y++) {
            visitQueue.add(1 + stride * y);
            visitQueue.add(width + stride * y);
        }
        traverse(visited, visitQueue, null);
        for(int x = 0; x < width; x++) {
            for(int y = 0; y < height; y++) {
                if(binary.unsafe_get(x, y) > 0) {
                    visited[x + 1][y + 1] = false;
                }
            }
        }
        Collection<Blob> blobs = new ArrayList<>();
        for(int x = 0; x < width; x++) {
            for(int y = 0; y < height; y++) {
                if(!visited[x + 1][y + 1]) {
                    LongAdder sumX = new LongAdder();
                    LongAdder sumY = new LongAdder();
                    LongAdder countAdder = new LongAdder();
                    visitQueue.add(x + 1 + (y + 1) * stride);
                    traverse(visited, visitQueue, (matchX, matchY) -> {
                        binary.unsafe_set(matchX - 1, matchY - 1, 1);
                        sumX.add(matchX);
                        //noinspection SuspiciousNameCombination
                        sumY.add(matchY);
                        countAdder.increment();
                    });
                    int count = countAdder.intValue();
                    if(count > 64) {
                        blobs.add(new Blob(sumX.intValue() / count, sumY.intValue() / count, count));
                    }
                }
            }
        }
        log.debug("Image processing took " + (System.currentTimeMillis() - startTime) + " ms");
        return blobs;
    }

    private static void traverse(boolean[][] visited, Queue<Integer> visitQueue,
                                BiConsumer<Integer, Integer> matchConsumer) {
        int stride = visited.length;
        while(true) {
            Integer position = visitQueue.poll();
            if(position == null) {
                break;
            }
            int x = position % stride;
            int y = position / stride;
            if(visited[x][y]) {
                continue;
            }
            visited[x][y] = true;
            if(matchConsumer != null) {
                matchConsumer.accept(x, y);
            }
            if(!visited[x - 1][y]) {
                visitQueue.add(position - 1);
            }
            if(!visited[x + 1][y]) {
                visitQueue.add(position + 1);
            }
            if(!visited[x][y - 1]) {
                visitQueue.add(position - stride);
            }
            if(!visited[x][y + 1]) {
                visitQueue.add(position + stride);
            }
        }
    }

    private static boolean[][] buildVisitedMap(GrayU8 image) {
        int width = image.getWidth();
        int height = image.getHeight();
        boolean[][] visited = new boolean[width + 2][height + 2];
        for(int x = 0; x < width + 2; x++) {
            visited[x][0] = true;
            visited[x][height + 1] = true;
        }
        for(int y = 1; y <= height; y++) {
            visited[0][y] = true;
            visited[width + 1][y] = true;
        }
        for(int x = 0; x < width; x++) {
            for(int y = 0; y < height; y++) {
                visited[x + 1][y + 1] = image.unsafe_get(x, y) > 0;
            }
        }
        return visited;
    }

    private static float distanceFromRange(float min, float max, float value) {
        if(value < min) {
            return min - value;
        }
        if(value > max) {
            return value - max;
        }
        return 0.0f;
    }

    private static float pow(float a, float b) {
        return (float) Math.pow(a, b);
    }

    private static float limit(float min, float max, float value) {
        return Math.min(Math.max(value, min), max);
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
