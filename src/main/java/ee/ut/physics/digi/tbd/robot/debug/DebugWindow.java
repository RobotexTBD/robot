package ee.ut.physics.digi.tbd.robot.debug;

import ee.ut.physics.digi.tbd.robot.context.RobotContext;
import ee.ut.physics.digi.tbd.robot.context.RobotContextHolder;
import ee.ut.physics.digi.tbd.robot.image.Image;
import ee.ut.physics.digi.tbd.robot.util.BeanUtil;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.SneakyThrows;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class DebugWindow extends Application {

    private static DebugWindow instance;
    private static final CountDownLatch latch = new CountDownLatch(1);

    private final ConfigManager configManager = RobotContextHolder.getContext().getConfigManager();
    private final Map<String, WritableImage> images = new HashMap<>();
    private final ObservableList<String> imageNames = FXCollections.observableArrayList(Collections.singletonList(""));
    private final RobotContext context = RobotContextHolder.getContext();

    private Parent scene;

    @SuppressWarnings({ "ConstantConditions", "unchecked" })
    @Override
    public void start(Stage primaryStage) throws IOException {
        instance = this;
        primaryStage.setTitle("Debug");
        scene = ((Parent) loadFxml("javafx/main.fxml"));
        primaryStage.setScene(new Scene(scene));
        primaryStage.setOnCloseRequest(t -> {
            Platform.exit();
            System.exit(0);
        });
        primaryStage.show();
        scene.lookupAll("#images .imageChoice").stream()
             .map(node -> (ComboBox) node)
             .forEach(comboBox -> comboBox.setItems(imageNames));
        ComboBox<String> configChoice = (ComboBox) scene.lookup("#configChoice");
        initConfigChoice(configChoice);
        Button saveButton = (Button) scene.lookup("#saveButton");
        saveButton.setOnAction(event -> {
            String key = configChoice.getValue();
            if(key != null) {
                configManager.saveConfiguration(key);
            }
        });
        Button revertButton = (Button) scene.lookup("#revertButton");
        revertButton.setOnAction(event -> {
            String key = configChoice.getValue();
            if(key != null) {
                configManager.revertConfiguration(key);
                Platform.runLater(() -> renderConfigurableComponent(key));
            }
        });
        latch.countDown();
    }

    @SuppressWarnings("unchecked")
    private void initConfigChoice(ComboBox<String> configChoice) {
        Map<String, Object> configurableComponents = configManager.getConfigurableComponents();
        configChoice.setItems(FXCollections.observableList(new ArrayList<>(configurableComponents.keySet())));
        configManager.addConfigurableComponentListener(evt -> {
            Platform.runLater(() -> {
                configChoice.setItems(FXCollections.observableList(new ArrayList<>(configurableComponents.keySet())));
            });
        });
        configChoice.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            Platform.runLater(() -> renderConfigurableComponent(newValue));
        });
    }

    @SuppressWarnings("unchecked")
    public void setImage(Image image, String label) {
        if(!images.containsKey(label)) {
            Platform.runLater(() -> imageNames.add(label));
            List<Node> imagePanes = ((Pane) scene.lookup("#images")).getChildren();
            scene.lookupAll("#images .imageChoice").stream()
                 .map(node -> (ComboBox) node)
                 .filter(comboBox -> comboBox.getValue() == null)
                 .forEach(comboBox -> Platform.runLater(() -> comboBox.setValue(label)));
            Platform.runLater(() -> imageNames.remove(""));
        }
        images.put(label, image.toWritableImage());
    }

    @SuppressWarnings("unchecked")
    public void renderImages() {
        List<Node> imagePanes = ((Pane) scene.lookup("#images")).getChildren();
        for(Node imagePane : imagePanes) {
            ImageView imageView = ((ImageView) imagePane.lookup(".image"));
            ComboBox<String> imageChoice = (ComboBox) imagePane.lookup(".imageChoice");
            String selected = imageChoice.getValue();
            if(selected != null) {
                imageView.setImage(images.get(selected));
            }
        }
    }

    public void renderConfigurableComponent(String key) {
        Object object = context.getConfigManager().getObject(key);
        VBox configContainer = (VBox) scene.lookup("#configContainer");
        ObservableList<Node> children = configContainer.getChildren();
        children.remove(0, children.size());
        for(Field field : context.getConfigManager().getConfigurableFields(object)) {
            Configurable annotation = field.getAnnotation(Configurable.class);
            if(BeanUtil.isInt(field)) {
                children.add(getIntegerConfigNode(object, field, annotation));
            } else if(BeanUtil.isFloat(field)) {
                children.add(getFloatConfigNode(object, field, annotation));
            } else if(BeanUtil.isString(field)) {
                children.add(getStringConfigNode(object, field, annotation));
            } else if(BeanUtil.isBoolean(field)) {
                children.add(getBooleanConfigNode(object, field, annotation));
            } else if(BeanUtil.isEnum(field)) {
                children.add(getEnumConfigNode(object, field, annotation));
            }
        }
    }

    private Node getIntegerConfigNode(Object object, Field field, Configurable annotation) {
        Node config = loadFxml("javafx/number.fxml");
        ((Label) config.lookup("Label")).setText(annotation.value());
        Slider slider = (Slider) config.lookup("Slider");
        TextField textField = ((TextField) config.lookup("TextField"));
        slider.setMin(annotation.minInt());
        slider.setMax(annotation.maxInt());
        slider.setBlockIncrement(1);
        slider.setMajorTickUnit(annotation.maxInt() - annotation.minInt());
        slider.setValue(BeanUtil.<Integer>get(object, field));
        textField.setText(Integer.toString(BeanUtil.<Integer>get(object, field)));
        slider.valueProperty().addListener((observable, oldValue, newValue) -> {
            BeanUtil.set(object, field, newValue.intValue());
            textField.setText(Integer.toString(newValue.intValue()));
        });
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                int value = Integer.parseInt(newValue);
                if(value < (int) slider.getMin()) {
                    value = (int) slider.getMin();
                    textField.setText(Integer.toString(value));
                } else if(value > (int) slider.getMax()) {
                    value = (int) slider.getMax();
                    textField.setText(Integer.toString(value));
                }
                slider.setValue(value);
                BeanUtil.set(object, field, value);
            } catch(NumberFormatException e) {
                textField.setText(oldValue);
            }
        });
        return config;
    }

    private Node getFloatConfigNode(Object object, Field field, Configurable annotation) {
        Node config = loadFxml("javafx/number.fxml");
        ((Label) config.lookup("Label")).setText(annotation.value());
        Slider slider = (Slider) config.lookup("Slider");
        TextField textField = ((TextField) config.lookup("TextField"));
        slider.setMin(annotation.minFloat());
        slider.setMax(annotation.maxFloat());
        slider.setBlockIncrement(0.0001f);
        slider.setMajorTickUnit(annotation.maxFloat() - annotation.minFloat());
        slider.setValue(BeanUtil.<Float>get(object, field));
        textField.setText(Float.toString(BeanUtil.<Float>get(object, field)));
        slider.valueProperty().addListener((observable, oldValue, newValue) -> {
            BeanUtil.set(object, field.getName(), newValue);
            textField.setText(Float.toString(newValue.floatValue()));
        });
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                float value = Float.parseFloat(newValue);
                if(value < (float) slider.getMin()) {
                    value = (float) slider.getMin();
                    textField.setText(Float.toString(value));
                } else if(value > (float) slider.getMax()) {
                    value = (float) slider.getMax();
                    textField.setText(Float.toString(value));
                }
                BeanUtil.set(object, field, value);
                slider.setValue(value);
            } catch(NumberFormatException e) {
                textField.setText(oldValue);
            }
        });
        return config;
    }

    private Node getStringConfigNode(Object object, Field field, Configurable annotation) {
        Node config = loadFxml("javafx/string.fxml");
        ((Label) config.lookup("Label")).setText(annotation.value());
        TextField textField = ((TextField) config.lookup("TextField"));
        textField.setText(BeanUtil.get(object, field));
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            BeanUtil.set(object, field, newValue);
        });
        return config;
    }

    private Node getBooleanConfigNode(Object object, Field field, Configurable annotation) {
        Node config = loadFxml("javafx/boolean.fxml");
        ((Label) config.lookup("Label")).setText(annotation.value());
        CheckBox checkbox = ((CheckBox) config.lookup("CheckBox"));
        checkbox.setSelected(BeanUtil.get(object, field));
        checkbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            BeanUtil.set(object, field, newValue);
        });
        return config;
    }

    @SuppressWarnings("unchecked")
    private Node getEnumConfigNode(Object object, Field field, Configurable annotation) {
        Class<? extends Enum> clazz = (Class<? extends Enum>) field.getDeclaringClass();
        Node config = loadFxml("javafx/enum.fxml");
        ((Label) config.lookup("Label")).setText(annotation.value());
        ChoiceBox<String> choicebox = ((ChoiceBox) config.lookup("ChoiceBox"));
        ObservableList<String> items = FXCollections.observableArrayList();
        for(Enum value : clazz.getEnumConstants()) {
            items.add(value.name());
        }
        choicebox.setItems(items);
        choicebox.setValue(BeanUtil.<Enum>get(object, field).name());
        choicebox.selectionModelProperty().addListener((observable, oldValue, newValue) -> {
            Enum result = null;
            for(Enum value : clazz.getEnumConstants()) {
                if(value.name().equals(newValue.getSelectedItem())) {
                    result = value;
                    break;
                }
            }
            BeanUtil.set(object, field, result);
        });
        return config;
    }

    @SuppressWarnings("ConstantConditions")
    @SneakyThrows
    private Node loadFxml(String file) {
        return FXMLLoader.load(getClass().getClassLoader().getResource(file));
    }

    @SneakyThrows
    public static DebugWindow getInstance() {
        if(instance == null) {
            new Thread(Application::launch, "Application runner").start();
        }
        latch.await();
        return instance;
    }

}
