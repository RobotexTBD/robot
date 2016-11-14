package ee.ut.physics.digi.tbd.robot.image;

import ee.ut.physics.digi.tbd.robot.matrix.Matrix;
import javafx.scene.image.WritableImage;

public interface Image extends Matrix {

    WritableImage toWritableImage();

}
