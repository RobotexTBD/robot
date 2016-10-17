package ee.ut.physics.digi.tbd.robot;

import ee.ut.physics.digi.tbd.robot.model.Image;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import lombok.SneakyThrows;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class DebugWindow extends Application {

    private static DebugWindow instance;
    private static final CountDownLatch latch = new CountDownLatch(1);

    private Map<ImagePanel, WritableImage> images = new EnumMap<>(ImagePanel.class);
    private Map<ImagePanel, String> labels = new EnumMap<>(ImagePanel.class);
    private Parent scene;

    @SuppressWarnings("ConstantConditions")
    @Override
    public void start(Stage primaryStage) throws IOException {
        instance = this;
        primaryStage.setTitle("Debug");
        scene = FXMLLoader.load(getClass().getClassLoader().getResource("scene.fxml"));
        primaryStage.setScene(new Scene(scene));
        primaryStage.setOnCloseRequest(t -> {
            Platform.exit();
            System.exit(0);
        });
        primaryStage.show();
        latch.countDown();
    }

    public void setImage(ImagePanel target, Image image, String label) {
        images.put(target, image.toWritableImage());
        if(label != null) {
            labels.put(target, label);
        } else {
            labels.remove(target);
        }
    }

    public void render() {
        for(ImagePanel imagePanel : ImagePanel.values()) {
            if(!images.containsKey(imagePanel)) {
                continue;
            }
            ImageView imageView = (ImageView)scene.lookup("#" + imagePanel.getId());
            imageView.setImage(images.get(imagePanel));
            Label imageLabel = (Label)scene.lookup("#" + imagePanel.getId() + "Label");
            imageLabel.setText(labels.getOrDefault(imagePanel, ""));
        }
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
