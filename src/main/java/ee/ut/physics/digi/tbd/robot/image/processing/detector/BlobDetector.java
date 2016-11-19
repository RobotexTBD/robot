package ee.ut.physics.digi.tbd.robot.image.processing.detector;

import ee.ut.physics.digi.tbd.robot.image.GrayscaleImage;
import ee.ut.physics.digi.tbd.robot.image.blob.Blob;

import java.util.Collection;

public interface BlobDetector {

    Collection<Blob> findBlobs(GrayscaleImage certaintyMap);

}
