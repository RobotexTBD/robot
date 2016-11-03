package ee.ut.physics.digi.tbd.robot;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Properties;

@Slf4j
public final class Settings {

    private static final String PROPERTIES_FILE_NAME = "application.properties";
    private static final String FIELD_ID_KEY = "radio.field.id";
    private static final String ROBOT_ID_KEY = "radio.robot.id";
    private static final String MAINBOARD_PORT_KEY = "mainboard.port.name";
    private static final String RADIO_PORT_KEY = "radio.port.name";
    private static final String WEBCAM_NAME_KEY = "webcam.name";
    private static final String MAINBOARD_USE_MOCK_KEY = "mainboard.useMock";
    private static final String REFEREE_USE_MOCK_KEY = "referee.useMock";
    private static final String SHOW_DEBUG_WINDOW_KEY = "debug.showWindow";
    private static Settings instance;
    private Properties properties;

    private Settings() {
        properties = new Properties();
        try {
            properties.load(this.getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE_NAME));
        } catch(IOException | NullPointerException e) {
            throw new IllegalStateException("Unable to read properties from \"" + PROPERTIES_FILE_NAME + "\"", e);
        }
    }

    public String getMainboardPortName() {
        return readStringProperty(MAINBOARD_PORT_KEY, "Mainboard port");
    }

    public String getRadioPortName() {
        return readStringProperty(RADIO_PORT_KEY, "Radio port");
    }

    public String getWebcamName() {
        return readStringProperty(WEBCAM_NAME_KEY, "Webcam");
    }

    public char getFieldId() {
        return readCharProperty(FIELD_ID_KEY, "Field ID");
    }

    public char getRobotId() {
        return readCharProperty(ROBOT_ID_KEY, "Robot ID");
    }

    public boolean shouldUseMainboardMock() {
        return readBooleanProperty(MAINBOARD_USE_MOCK_KEY);
    }

    public boolean shouldUseRefereeMock() {
        return readBooleanProperty(REFEREE_USE_MOCK_KEY);
    }

    public boolean shouldShowDebugWindow() {
        return readBooleanProperty(SHOW_DEBUG_WINDOW_KEY);
    }

    private char readCharProperty(String key, String name) {
        String propertyValue = readStringProperty(key, name);
        if(propertyValue.length() != 1) {
            throw new IllegalStateException(name + " should be a single char (got \"" + propertyValue + "\" instead)");
        }
        return propertyValue.charAt(0);
    }

    private String readStringProperty(String key, String name) {
        String propertyValue = properties.getProperty(key);
        if(propertyValue == null) {
            throw new IllegalStateException(name + " not specified (key: \"" + key + "\")");
        }
        return propertyValue;
    }

    @SuppressWarnings("SimplifiableIfStatement")
    private boolean readBooleanProperty(String key) {
        String propertyValue = properties.getProperty(key);
        if(propertyValue == null) {
            return false;
        }
        return propertyValue.equalsIgnoreCase("true") || propertyValue.equals("1");
    }

    public static Settings getInstance() {
        if(instance == null) {
            instance = new Settings();
        }
        return instance;
    }

}
