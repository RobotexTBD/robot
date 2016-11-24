package ee.ut.physics.digi.tbd.robot.util;

import ee.ut.physics.digi.tbd.robot.image.blob.Blob;

import java.util.Arrays;
import java.util.Collection;

public final class BlobUtil {

    private BlobUtil() {}

    public static Blob join(Blob... blobs) {
        return join(Arrays.asList(blobs));
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public static Blob join(Collection<Blob> blobs) {
        if(blobs.isEmpty()) {
            return null;
        }
        int size = blobs.stream().mapToInt(Blob::getSize).sum();
        return new Blob(blobs.stream().mapToInt(Blob::getMinX).min().getAsInt(),
                        blobs.stream().mapToInt(blob -> blob.getCenterX() * blob.getSize()).sum() / size,
                        blobs.stream().mapToInt(Blob::getMaxX).max().getAsInt(),
                        blobs.stream().mapToInt(Blob::getMinY).min().getAsInt(),
                        blobs.stream().mapToInt(blob -> blob.getCenterY() * blob.getSize()).sum() / size,
                        blobs.stream().mapToInt(Blob::getMaxY).max().getAsInt(),
                        size);
    }

}
