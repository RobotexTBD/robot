package ee.ut.physics.digi.tbd.robot.image.blob;

import lombok.Getter;

public class BlobBuilder {

    private int minX = Integer.MAX_VALUE;
    private long sumX = 0;
    private int maxX = Integer.MIN_VALUE;

    private int minY = Integer.MAX_VALUE;
    private long sumY = 0;
    private int maxY = Integer.MIN_VALUE;

    @Getter
    private int size;

    public void addPoint(int x, int y) {
        addPointX(x);
        addPointY(y);
        size++;
    }

    public Blob toBlob() {
        int centerX = (int) (sumX / size);
        int centerY = (int) (sumY / size);
        return new Blob(minX, centerX, maxX, minY, centerY, maxY, size);
    }

    private void addPointX(int x) {
        minX = Math.min(x, minX);
        maxX = Math.max(x, maxX);
        sumX += x;
    }

    private void addPointY(int y) {
        minY = Math.min(y, minY);
        maxY = Math.max(y, maxY);
        sumY += y;
    }

}
