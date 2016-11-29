package ee.ut.physics.digi.tbd.robot.util;

import lombok.SneakyThrows;
import org.apache.commons.beanutils.PropertyUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

public final class BeanUtil {

    private BeanUtil() {}

    public static boolean isInt(Field field) {
        return Integer.class.isAssignableFrom(field.getType()) || int.class.isAssignableFrom(field.getType());
    }

    public static boolean isFloat(Field field) {
        return Float.class.isAssignableFrom(field.getType()) || float.class.isAssignableFrom(field.getType());
    }

    public static boolean isBoolean(Field field) {
        return Boolean.class.isAssignableFrom(field.getType()) || boolean.class.isAssignableFrom(field.getType());
    }

    public static boolean isString(Field field) {
        return String.class.isAssignableFrom(field.getType());
    }

    public static boolean isEnum(Field field) {
        return Enum.class.isAssignableFrom(field.getType());
    }

    public static boolean set(Object target, Field field, Object value) {
        return set(target, field.getName(), value);
    }

    public static boolean set(Object target, String field, Object value) {
        try {
            PropertyUtils.setProperty(target, field, value);
            return true;
        } catch(InvocationTargetException e) {
            return false;
        } catch(NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalArgumentException("Unable to invoke setter for " + target.getClass().getSimpleName() + "" +
                                               "::" + field, e);
        }
    }

    public static <T> T get(Object target, Field field) {
        return get(target, field.getName());
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    public static <T> T get(Object target, String field) {
        try {
            return (T) PropertyUtils.getProperty(target, field);
        } catch(NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException("Unable to invoke getter for " + target.getClass().getSimpleName() +
                                               "::" + field, e);
        }
    }

}
