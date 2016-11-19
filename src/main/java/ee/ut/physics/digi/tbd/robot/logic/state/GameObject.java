package ee.ut.physics.digi.tbd.robot.logic.state;

import ee.ut.physics.digi.tbd.robot.image.blob.Blob;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class GameObject {

    /**
     * Raw blob data from image processing
     */
    private Blob blob;

    /**
     * Direction of object's center mass in degrees. Positive value means the object is to the right hand side, negative
     * means to the left side.
     */
    private float direction;

    /**
     * Distance in meters.
     */
    private float distance;

    private GameObjectType type;

}
