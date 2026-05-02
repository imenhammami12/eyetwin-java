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
    requires uk.co.caprica.vlcj;
    requires com.gluonhq.maps;
    requires javafx.swing;
    requires com.google.zxing.javase;
    requires com.google.zxing;
    requires com.google.gson;
    requires org.java_websocket;

    opens com.eyetwin to javafx.fxml;
    opens com.eyetwin.controller to javafx.fxml;
    opens com.eyetwin.controller.admin to javafx.fxml;
    opens com.eyetwin.entities to com.google.gson, javafx.base, javafx.fxml;
    opens com.eyetwin.websocket.model to com.google.gson;

    exports com.eyetwin;
    exports com.eyetwin.config;
    exports com.eyetwin.tools;
    exports com.eyetwin.services;
    exports com.eyetwin.entities;
    exports com.eyetwin.controller;
    exports com.eyetwin.interfaces;
}