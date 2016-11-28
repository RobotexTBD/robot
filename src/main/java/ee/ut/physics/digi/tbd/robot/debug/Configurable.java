package ee.ut.physics.digi.tbd.robot.debug;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Configurable {

    String value();

    int minInt() default 0;
    int maxInt() default 0;
    float minFloat() default 0.0f;
    float maxFloat() default 0.0f;

}
