package ee.ut.physics.digi.tbd.robot.model;

public interface Matrix {

    int getHeight();
    int getWidth();

    default int getIndex(int x, int y) {
        return getStartIndex() + x + y * getStride();
    }

    default int getStartIndex() {
        return 0;
    }

    default int getStride() {
        return getWidth();
    }

    default int getElementCount() {
        return getHeight() * getWidth();
    }


}
