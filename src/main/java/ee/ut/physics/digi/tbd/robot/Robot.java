package ee.ut.physics.digi.tbd.robot;

import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.io.webcamcapture.UtilWebcamCapture;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;
import com.github.sarxos.webcam.Webcam;
import javafx.application.Platform;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.Collection;

@Slf4j
public class Robot implements Runnable {

    private boolean isRunning;
    private DebugWindow debugWindow;
    private static final boolean DEBUG = true;

    public static void main(String[] args) {
        Robot robot = new Robot();
        robot.run();
    }

    public boolean isDebug() {
        return DEBUG;
    }

    @Override
    @SneakyThrows
    public void run() {
        isRunning = true;
        Webcam webcam = UtilWebcamCapture.openDevice("ManyCam", 640, 480);
        if(isDebug()) {
            debugWindow = DebugWindow.getInstance();
        }
        while(isRunning) {
            long startTime = System.currentTimeMillis();
            loop(webcam.getImage());
            if(isDebug()) {
                Platform.runLater(debugWindow::render);
            }
            //log.debug("Loop took " + (System.currentTimeMillis() - startTime) + " ms");
        }
        if(isDebug()) {
            debugWindow.stop();
        }

    }

    public void loop(BufferedImage original) {

        Planar<GrayF32> hsv = ImageConverterUtil.getHsvImage(original);
        setDebugImage(ImagePanel.ORIGINAL, hsv, "Original");
        GrayF32 certaintyMap = ImageProcessor.generateCertaintyMap(hsv);
        setDebugImage(ImagePanel.MUTATED2, certaintyMap, "Certainty map");
        Collection<Blob> blobs = ImageProcessor.findBlobs(certaintyMap);

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

}
