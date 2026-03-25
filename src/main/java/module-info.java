module org.example.eyetwinjava {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.eyetwinjava to javafx.fxml;
    exports org.example.eyetwinjava;
}