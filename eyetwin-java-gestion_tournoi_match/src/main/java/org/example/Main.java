package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.utils.MyDatabase;
import java.sql.Connection;
import java.net.URL;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        System.out.println("Starting application...");
        
        // Test database connection
        MyDatabase db = MyDatabase.getInstance();
        Connection conn = db.getConnection();
        
        String dbStatus = (conn != null) ? "Database Connected Successfully!" : "Database Connection Failed.";
        System.out.println("Main: " + dbStatus);

        try {
            URL resource = getClass().getResource("/org/example/login.fxml");
            if (resource == null) {
                System.out.println("Cannot find FXML file");
            } else {
                Parent root = FXMLLoader.load(resource);
                Scene scene = new Scene(root, 1050, 700);

                primaryStage.setTitle("Login - EyeTwin Platform");
                primaryStage.setMinWidth(900);
                primaryStage.setMinHeight(600);
                primaryStage.setScene(scene);
                primaryStage.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}