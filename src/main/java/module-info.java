module org.example.eyetwinjava {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.eyetwin to javafx.fxml;
    exports com.eyetwin;
}