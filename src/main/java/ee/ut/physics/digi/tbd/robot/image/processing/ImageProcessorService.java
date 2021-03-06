package ee.ut.physics.digi.tbd.robot.image.processing;

import ee.ut.physics.digi.tbd.robot.image.BinaryImage;
import ee.ut.physics.digi.tbd.robot.image.ColoredImage;
import ee.ut.physics.digi.tbd.robot.image.GrayscaleImage;
import ee.ut.physics.digi.tbd.robot.image.processing.kernel.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.Callable;

@Slf4j
public class ImageProcessorService {

    private final CertaintyMapKernel ballDetector;
    private final CertaintyMapKernel yellowDetector;
    private final CertaintyMapKernel blueDetector;
    private final RgbToHsvConverterKernel rgbToHsvConverter;
    private final HsvToRgbConverterKernel hsvToRgbConverter;
    private final ThresholderKernel thresholder;
    private final WhiteBalanceKernel whitebalance;

    public ImageProcessorService(int width, int height) {
        try {
            ballDetector = new BallDetectorKernel(width, height);
            yellowDetector = new YellowDetectorKernel(width, height);
            blueDetector = new BlueDetectorKernel(width, height);
            rgbToHsvConverter = new RgbToHsvConverterKernel(width, height);
            hsvToRgbConverter = new HsvToRgbConverterKernel(width, height);
            thresholder = new ThresholderKernel(width, height);
            whitebalance = new WhiteBalanceKernel(width, height);
        } catch(IOException e) {
            throw new IllegalStateException("Unable to initialize image processor service", e);
        }
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

    public GrayscaleImage generateBallCertaintyMap(ColoredImage hsvImage) {
        return measureTimeAndReturn("Ball certainty map generation",
                                    () -> ballDetector.generateCertaintyMap(hsvImage));
    }


    public GrayscaleImage generateBlueCertaintyMap(ColoredImage hsvImage) {
        return measureTimeAndReturn("Blue certainty map generation",
                                    () -> blueDetector.generateCertaintyMap(hsvImage));
    }


    public GrayscaleImage generateYellowCertaintyMap(ColoredImage hsvImage) {
        return measureTimeAndReturn("Yellow certainty map generation",
                                    () -> yellowDetector.generateCertaintyMap(hsvImage));
    }

    public ColoredImage whiteBalance(ColoredImage rgbImage) {
        return measureTimeAndReturn("White balance",
                                    () -> whitebalance.balance(rgbImage));
    }

}
