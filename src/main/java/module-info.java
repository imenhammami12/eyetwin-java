module org.example.eyetwinjava {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires jbcrypt;
    requires javafx.web;
    requires jdk.jsobject;
    requires bcrypt;
    requires java.prefs;

    opens com.eyetwin to javafx.fxml;
    opens com.eyetwin.controller to javafx.fxml;

    exports com.eyetwin;
    exports com.eyetwin.config;
    exports com.eyetwin.util;
    exports com.eyetwin.service;
    exports com.eyetwin.dao;
    exports com.eyetwin.model;
    exports com.eyetwin.controller;
}