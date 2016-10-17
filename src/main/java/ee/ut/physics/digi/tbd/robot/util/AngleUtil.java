package ee.ut.physics.digi.tbd.robot.util;

public class AngleUtil {

    private static final float PI = (float) Math.PI;

    private AngleUtil() {}

    public static float toDegrees(float radians) {
        return radians * 180 / PI;
    }

    public static float toRadians(float degrees) {
        return degrees * PI / 180;
    }


}
