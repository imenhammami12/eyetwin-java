package com.eyetwin;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        stage.setTitle("EyeTwin E-Sport Platform");
        // Pour l'instant on garde hello-view.fxml pour tester
        FXMLLoader loader = new FXMLLoader(getClass().getResource(
                "/org/example/eyetwinjava/hello-view.fxml"));
        Scene scene = new Scene(loader.load());
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static Stage getPrimaryStage() { return primaryStage; }

    public static void main(String[] args) { launch(args); }
}