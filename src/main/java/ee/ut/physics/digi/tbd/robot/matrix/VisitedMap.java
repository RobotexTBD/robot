package ee.ut.physics.digi.tbd.robot.matrix;

import lombok.Getter;

@Getter
public class VisitedMap implements Matrix {

    private final int stride;
    private final boolean[] visited;
    private final int width;
    private final int height;

    public VisitedMap(int width, int height) {
        stride = width + 2;
        visited = new boolean[stride * (height + 2)];
        this.width = width;
        this.height = height;
        fillEdges(width, height);
    }

    private void fillEdges(int width, int height) {
        fillTopAndBottom(width, height);
        fillLeftAndRight(height);
    }

    private void fillLeftAndRight(int height) {
        int left = stride;
        int right = 2 * stride - 1;
        for(int y = 0; y < height; y++) {
            visited[left] = true;
            visited[right] = true;
            left += stride;
            right += stride;
        }
    }

    private void fillTopAndBottom(int width, int height) {
        int top = 1;
        int bottom = (height + 1) * stride + 1;
        for(int x = 0; x < width; x++) {
            visited[top] = true;
            visited[bottom] = true;
            top++;
            bottom++;
        }
    }

    @Override
    public int getStartIndex() {
        return stride + 1;
    }

}
