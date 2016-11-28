package ee.ut.physics.digi.tbd.robot.debug;


import ee.ut.physics.digi.tbd.robot.util.BeanUtil;
import ee.ut.physics.digi.tbd.robot.util.PropertiesUtil;
import lombok.extern.slf4j.Slf4j;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class ConfigManager {

    private Map<String, Object> configurableComponents = new HashMap<>();
    private List<PropertyChangeListener> configurableComponentsListeners = new ArrayList<>();

    public void registerConfigurableComponent(Object object, String key) {
        configurableComponents.put(key, object);
        configurableComponentsListeners.forEach(listener -> listener.propertyChange(null));
        loadConfiguration(key, object);
    }

    public void saveConfiguration(String key) {
        Object object = configurableComponents.get(key);
        new File("./config/").mkdirs();
        File propertiesFile = new File("./config/" + key + ".properties");
        try(FileOutputStream outputStream = new FileOutputStream(propertiesFile)) {
            Properties properties = new Properties();
            for(Field field : getConfigurableFields(object)) {
                String value = null;
                if(BeanUtil.isInt(field)) {
                    value = Integer.toString(BeanUtil.<Integer>get(object, field));
                } else if(BeanUtil.isFloat(field)) {
                    value = Float.toString(BeanUtil.<Float>get(object, field));
                } else if(BeanUtil.isString(field)) {
                    value = BeanUtil.get(object, field);
                } else if(BeanUtil.isBoolean(field)) {
                    value = Boolean.toString(BeanUtil.<Boolean>get(object, field));
                } else if(BeanUtil.isEnum(field)) {
                    value = BeanUtil.<Enum>get(object, field).name();
                }
                if(value == null) {
                    value = "";
                }
                properties.setProperty(field.getName(), value);
            }
            properties.store(outputStream, null);
        } catch(IOException e) {
            log.warn("Unable to write config for component \"" + key + "\"");
        }
    }

    public void revertConfiguration(String key) {
        if(!configurableComponents.containsKey(key)) {
            throw new IllegalArgumentException("No component registered with key \"" + key + "\"");
        }
        loadConfiguration(key, configurableComponents.get(key));
    }

    @SuppressWarnings("unchecked")
    private void loadConfiguration(String key, Object object) {
        File propertiesFile = new File("./config/" + key + ".properties");
        try(FileInputStream inputStream = new FileInputStream(propertiesFile)) {
            Properties properties = new Properties();
            properties.load(inputStream);
            for(Field field : getConfigurableFields(object)) {
                Object value = null;
                if(BeanUtil.isInt(field)) {
                    value = PropertiesUtil.readIntProperty(properties, field.getName(), 0);
                } else if(BeanUtil.isFloat(field)) {
                    value = PropertiesUtil.readFloatProperty(properties, field.getName(), 0.0f);
                } else if(BeanUtil.isString(field)) {
                    value = PropertiesUtil.readStringProperty(properties, field.getName(), "");
                } else if(BeanUtil.isBoolean(field)) {
                    value = PropertiesUtil.readBooleanProperty(properties, field.getName(), true);
                } else if(BeanUtil.isEnum(field)) {
                    value = PropertiesUtil.readEnumProperty(properties, field.getName(), field.getType(),
                                                            ((Class<Enum>) field.getType()).getEnumConstants()[0]);
                }
                BeanUtil.set(object, field, value);
            }
        } catch(IOException e) {
            log.warn("Unable to read config for component \"" + key + "\", creating default config");
            saveConfiguration(key);
        }
    }

    public Map<String, Object> getConfigurableComponents() {
        return Collections.unmodifiableMap(configurableComponents);
    }

    public void addConfigurableComponentListener(PropertyChangeListener listener) {
        configurableComponentsListeners.add(listener);
    }


    public void removeConfigurableComponentListener(PropertyChangeListener listener) {
        configurableComponentsListeners.remove(listener);
    }

    public Object getObject(String key) {
        return configurableComponents.get(key);
    }

    public List<Field> getConfigurableFields(Object object) {
        return Arrays.stream(object.getClass().getDeclaredFields())
                     .filter(field -> field.isAnnotationPresent(Configurable.class))
                     .collect(Collectors.toList());
    }



}
