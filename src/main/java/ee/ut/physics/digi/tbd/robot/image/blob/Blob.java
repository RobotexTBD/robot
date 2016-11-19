package ee.ut.physics.digi.tbd.robot.image.blob;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Blob {

    private int minX;
    private int centerX;
    private int maxX;
    private int minY;
    private int centerY;
    private int maxY;
    private int size;

}
