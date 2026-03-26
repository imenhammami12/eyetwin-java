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
        stage.setWidth(1280);
        stage.setHeight(800);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setResizable(true);
        navigateTo("/com/eyetwin/views/login.fxml", "Login");
    }

    public static void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    MainApp.class.getResource(fxmlPath)
            );
            Scene scene = new Scene(loader.load(), 1280, 800);
            primaryStage.setScene(scene);
            primaryStage.setTitle("EyeTwin — " + title);
            primaryStage.show();
        } catch (Exception e) {
            System.err.println("❌ Erreur : " + fxmlPath);
            e.printStackTrace();
        }
    }

    public static Stage getPrimaryStage() { return primaryStage; }

    public static void main(String[] args) { launch(args); }
}