module org.example.eyetwinjava {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens com.eyetwin to javafx.fxml;
    exports com.eyetwin;
    exports com.eyetwin.config;
    exports com.eyetwin.util;
}