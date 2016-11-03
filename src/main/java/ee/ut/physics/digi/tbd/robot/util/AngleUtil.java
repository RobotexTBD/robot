package ee.ut.physics.digi.tbd.robot.util;

public final class AngleUtil {

    private static final float PI = (float) Math.PI;
    public static final int HALF_CIRCLE_DEGREES = 180;

    private AngleUtil() {}

    public static float toDegrees(float radians) {
        return radians * HALF_CIRCLE_DEGREES / PI;
    }

    public static float toRadians(float degrees) {
        return degrees * PI / HALF_CIRCLE_DEGREES;
    }


}
