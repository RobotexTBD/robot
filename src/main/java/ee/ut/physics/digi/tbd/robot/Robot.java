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

import java.awt.image.BufferedImage;

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
        setDebugImage(ImagePanel.ORIGINAL, original, "Original");

        Planar<GrayF32> hsv = ImageConverterUtil.getHsvImage(original);
        GrayF32 certaintyMap = ImageProcessor.generateCertaintyMap(hsv);
        setDebugImage(ImagePanel.MUTATED3, certaintyMap, "Certainty map");

        GrayU8 maxCertainty = ThresholdImageOps.threshold(certaintyMap, null, 1.0f, false);
        setDebugImage(ImagePanel.MUTATED5, maxCertainty, "Max Certainty");

        ImageProcessor.fillHoles(maxCertainty);
        setDebugImage(ImagePanel.MUTATED4, maxCertainty, "Max Certainty filled");

        ImageProcessor.mutateImage(hsv);
        setDebugImage(ImagePanel.MUTATED2, hsv, "Mutated");

        GrayU8 binary = ThresholdImageOps.threshold(hsv.getBand(1), null, 0.8f, false);
        binary = BinaryImageOps.erode8(binary, 2, null);
        binary = BinaryImageOps.dilate8(binary, 2, null);
        setDebugImage(ImagePanel.MUTATED1, binary, "Thresholded");


     /*   Graphics2D g2 = original.createGraphics();
        g2.setStroke(new BasicStroke(2));
        g2.setColor(Color.RED);

        java.util.List<Contour> contours = BinaryImageOps.contour(binary, ConnectRule.EIGHT, null);
        for(Contour contour: contours) {
            if(contour.external.size() > 20) {
                continue;
            }
            EllipseRotated_F64 ellipse = ShapeFittingOps.fitEllipse_I32(contour.external, 100, false, null).shape;
            if(ellipse.a / ellipse.b > 1.5 || ellipse.a * ellipse.b < 3.0) {
                continue;
            }
            String text = String.valueOf(ellipse.a * ellipse.b);
            g2.drawChars(text.toCharArray(), 0, text.length(),
                         (int) ellipse.center.getX(), (int) ellipse.center.getY());
            VisualizeShapes.drawEllipse(ellipse, g2);
        }
        setDebugImage(ImagePanel.MUTATED3, original, "Detections");*/
    }

    private void setDebugImage(ImagePanel target, GrayU8 image, String label) {
        if(isDebug()) {
            debugWindow.setImage(target, ImageConverterUtil.getBufferedImage(image), label);
        }
    }

    private void setDebugImage(ImagePanel target, GrayF32 image, String label) {
        if(isDebug()) {
            debugWindow.setImage(target, ImageConverterUtil.getBufferedImage(image), label);
        }
    }

    private void setDebugImage(ImagePanel target, Planar<GrayF32> image, String label) {
        if(isDebug()) {
            debugWindow.setImage(target, ImageConverterUtil.getBufferedImage(image), label);
        }
    }

    private void setDebugImage(ImagePanel target, BufferedImage image, String label) {
        if(isDebug()) {
            debugWindow.setImage(target, image, label);
        }
    }

}
