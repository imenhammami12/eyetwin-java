module org.example.eyetwinjava {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires jbcrypt;
    requires javafx.graphics;
    requires javafx.web;
    requires jdk.jsobject;
    requires bcrypt;
    requires java.prefs;
    requires opencv;
    requires jakarta.mail;
    requires java.desktop;
    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;
    requires java.net.http;
    requires org.json;
    requires stripe.java;

    requires jdk.httpserver;
    requires javafx.media;

    opens com.eyetwin to javafx.fxml;
    opens com.eyetwin.controller to javafx.fxml;
    opens com.eyetwin.controller.admin to javafx.fxml;

    exports com.eyetwin;
    exports com.eyetwin.config;
    exports com.eyetwin.tools;
    exports com.eyetwin.services;
    exports com.eyetwin.entities;
    exports com.eyetwin.controller;
    exports com.eyetwin.interfaces;

}