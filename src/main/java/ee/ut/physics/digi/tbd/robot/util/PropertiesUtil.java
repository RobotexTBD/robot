package ee.ut.physics.digi.tbd.robot.util;

import java.util.Properties;

import static jogamp.graph.font.typecast.ot.table.Table.name;


public final class PropertiesUtil {

    private PropertiesUtil() {}

    public static String readStringProperty(Properties properties, String key, String defaultValue) {
        String propertyValue = properties.getProperty(key);
        if(propertyValue == null) {
            if(defaultValue != null) {
                return defaultValue;
            }
            throw new IllegalStateException("\"" + key + "\" not present");
        }
        return propertyValue;
    }

    public static boolean readBooleanProperty(Properties properties, String key, boolean defaultValue) {
        String propertyValue = readStringProperty(properties, key, Boolean.toString(defaultValue));
        return propertyValue.equalsIgnoreCase("true") || propertyValue.equals("1");
    }

    public static int readIntProperty(Properties properties, String key, int defaultValue) {
        String propertyValue = readStringProperty(properties, key, Integer.toString(defaultValue));
        try {
            return Integer.parseInt(propertyValue);
        } catch(NumberFormatException e) {
            throw new IllegalStateException(name + " should be a valid integer (got \"" +
                                            propertyValue + "\" instead)");
        }
    }

    public static float readFloatProperty(Properties properties, String key, float defaultValue) {
        String propertyValue = readStringProperty(properties, key, Float.toString(defaultValue));
        try {
            return Float.parseFloat(propertyValue);
        } catch(NumberFormatException e) {
            throw new IllegalStateException(name + " should be a valid float (got \"" +
                                            propertyValue + "\" instead)");
        }
    }

    @SuppressWarnings("unchecked")
    public static Object readEnumProperty(Properties properties, String key, Class<?> clazz, Enum<?> defaultValue) {
        String propertyValue = readStringProperty(properties, key, "");
        Class<? extends Enum> enumClass = (Class<? extends Enum>) clazz;
        Enum[] values = enumClass.getEnumConstants();
        for(Enum value : values) {
            if(value.name().equals(propertyValue)) {
                return value;
            }
        }
        if(defaultValue != null) {
            return defaultValue;
        }
        throw new IllegalStateException("Invalid enum value");
    }

}
