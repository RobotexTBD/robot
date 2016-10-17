package ee.ut.physics.digi.tbd.robot;

import boofcv.io.webcamcapture.UtilWebcamCapture;
import ee.ut.physics.digi.tbd.robot.kernel.BallDetectorKernel;
import ee.ut.physics.digi.tbd.robot.kernel.RgbToHsvConverterKernel;
import ee.ut.physics.digi.tbd.robot.kernel.ThresholderKernel;
import ee.ut.physics.digi.tbd.robot.model.BinaryImage;
import ee.ut.physics.digi.tbd.robot.model.ColoredImage;
import ee.ut.physics.digi.tbd.robot.model.GrayscaleImage;
import ee.ut.physics.digi.tbd.robot.util.CameraReader;
import javafx.application.Platform;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Collection;

@Slf4j
public class Robot implements Runnable {

    private static final boolean DEBUG = true;

    private DebugWindow debugWindow;
    private final CameraReader cameraReader;
    private final RgbToHsvConverterKernel rgbToHsvConverterKernel;
    private final BallDetectorKernel ballDetectorKernel;
    private final ThresholderKernel thresholderKernel;

    public Robot(String cameraName, int width, int height) throws IOException {
        if(isDebug()) {
            debugWindow = DebugWindow.getInstance();
        }
        cameraReader = new CameraReader(UtilWebcamCapture.openDevice(cameraName, width, height));
        rgbToHsvConverterKernel = new RgbToHsvConverterKernel(width, height);
        ballDetectorKernel = new BallDetectorKernel(width, height);
        thresholderKernel = new ThresholderKernel(width, height);
    }

    public static void main(String[] args) throws IOException {
        new Robot("ManyCam", 640, 480).run();
    }

    public boolean isDebug() {
        return DEBUG;
    }

    @SuppressWarnings("InfiniteLoopStatement")
    @Override
    @SneakyThrows
    public void run() {
        while(true) {
            ColoredImage rgbImage = cameraReader.readRgbImage();
            long startTime = System.currentTimeMillis();
            loop(rgbImage);
            log.debug("Loop took " + (System.currentTimeMillis() - startTime) + " ms");
            if(isDebug()) {
                Platform.runLater(debugWindow::render);
            }
        }
    }

    public void loop(ColoredImage rgbImage) {
        ColoredImage hsvImage = rgbToHsvConverterKernel.convert(rgbImage);
        debugWindow.setImage(ImagePanel.ORIGINAL, rgbImage, "Original");
        GrayscaleImage certaintyMap = ballDetectorKernel.generateCertaintyMap(hsvImage);
        debugWindow.setImage(ImagePanel.MUTATED1, certaintyMap, "Certainty map");
        BinaryImage maxCertainty = thresholderKernel.threshold(certaintyMap, 205, 255);
        debugWindow.setImage(ImagePanel.MUTATED2, maxCertainty, "Max certainty");
        Collection<Blob> blobs;
    }

  /*  public void loop(BufferedImage original) {

        Planar<GrayF32> hsv = ImageConverterUtil.getHsvImage(original);
        setDebugImage(ImagePanel.ORIGINAL, hsv, "Original");
        GrayF32 certaintyMap = ImageProcessor.generateCertaintyMap(hsv);
        setDebugImage(ImagePanel.MUTATED2, certaintyMap, "Certainty map");

        GrayU8 maxCertainty = ThresholdImageOps.threshold(certaintyMap, null, 255.0f * 0.5f, false);
        maxCertainty = BinaryImageOps.dilate8(maxCertainty, 1, null);
        setDebugImage(ImagePanel.MUTATED5, maxCertainty, "Max Certainty");

        Collection<Blob> blobs = ImageProcessor.findBlobsAndFillHoles(maxCertainty);
        setDebugImage(ImagePanel.MUTATED4, maxCertainty, "Max Certainty filled");


        Graphics2D g2 = original.createGraphics();
        g2.setStroke(new BasicStroke(2));
        g2.setColor(Color.RED);
        for(Blob blob : blobs) {
            float radius = (float) Math.sqrt(blob.getSize() / Math.PI);
            g2.draw(new Ellipse2D.Float(blob.getCenterX() - radius, blob.getCenterY() - radius,
                                        radius * 2, radius * 2));
            String text = String.format("%dx%d: %d", blob.getCenterX(), blob.getCenterY(), blob.getSize());
            g2.drawChars(text.toCharArray(), 0, text.length(),
                         (int) (blob.getCenterX() + radius), (int) (blob.getCenterY() - radius));
        }
        setDebugImage(ImagePanel.MUTATED3, original, "Detections");

        GrayU8 binary = ThresholdImageOps.threshold(hsv.getBand(1), null, 0.8f, false);
        binary = BinaryImageOps.erode8(binary, 2, null);
        binary = BinaryImageOps.dilate8(binary, 2, null);
        setDebugImage(ImagePanel.MUTATED1, binary, "Thresholded");

    }

    private void setDebugImage(ImagePanel target, GrayU8 image, String label) {
        if(isDebug()) {
            debugWindow.setImage(target, ImageConverterUtil.getImage(image), label);
        }
    }

    private void setDebugImage(ImagePanel target, GrayF32 image, String label) {
        if(isDebug()) {
            debugWindow.setImage(target, ImageConverterUtil.getImage(image), label);
        }
    }

    private void setDebugImage(ImagePanel target, Planar<GrayF32> image, String label) {
        if(isDebug()) {
            debugWindow.setImage(target, ImageConverterUtil.getImage(image), label);
        }
    }

    private void setDebugImage(ImagePanel target, BufferedImage image, String label) {
        if(isDebug()) {
            debugWindow.setImage(target, image, label);
        }
    }
*/
}
