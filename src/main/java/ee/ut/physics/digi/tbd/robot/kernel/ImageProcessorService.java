package ee.ut.physics.digi.tbd.robot.kernel;

import ee.ut.physics.digi.tbd.robot.matrix.image.BinaryImage;
import ee.ut.physics.digi.tbd.robot.matrix.image.ColoredImage;
import ee.ut.physics.digi.tbd.robot.matrix.image.GrayscaleImage;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.Callable;

@Slf4j
public class ImageProcessorService {

    private final BallDetectorKernel ballDetector;
    private final RgbToHsvConverterKernel rgbToHsvConverter;
    private final HsvToRgbConverterKernel hsvToRgbConverter;
    private final ThresholderKernel thresholder;

    public ImageProcessorService(int width, int height) {
        try {
            ballDetector = new BallDetectorKernel(width, height);
            rgbToHsvConverter = new RgbToHsvConverterKernel(width, height);
            hsvToRgbConverter = new HsvToRgbConverterKernel(width, height);
            thresholder = new ThresholderKernel(width, height);
        } catch(IOException e) {
            throw new IllegalStateException("Unable to initialize image processor service", e);
        }
    }

    public GrayscaleImage generateBallCertaintyMap(ColoredImage hsvImage) {
        return measureTimeAndReturn("Ball certainty map generation", () -> ballDetector.generateCertaintyMap(hsvImage));
    }

    public ColoredImage convertRgbToHsv(ColoredImage rgbImage) {
        return measureTimeAndReturn("RGB -> HSV", () -> rgbToHsvConverter.convert(rgbImage));
    }

    public ColoredImage convertHsvToRgb(ColoredImage hsvImage) {
        return measureTimeAndReturn("HSV -> RGB", () -> hsvToRgbConverter.convert(hsvImage));
    }

    public BinaryImage threshold(GrayscaleImage image, float min, float max, int maxValue) {
        return measureTimeAndReturn("Thresholding", () -> thresholder.threshold(image, min, max, maxValue));
    }

    public BinaryImage threshold(GrayscaleImage image, int min, int max) {
        return measureTimeAndReturn("Thresholding", () -> thresholder.threshold(image, min, max));
    }

    @SneakyThrows
    private <T> T measureTimeAndReturn(String name, Callable<T> task) {
        long time = System.currentTimeMillis();
        T result = task.call();
        log.trace("Task \"" + name + "\" took " + (System.currentTimeMillis() - time) + " milliseconds");
        return result;
    }

}
