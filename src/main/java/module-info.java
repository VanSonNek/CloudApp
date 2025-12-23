module com.example.demo {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.base;
    requires java.net.http;
    requires com.google.gson;
    requires java.desktop;
    requires javafx.media;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.datatype.jsr310;


    opens com.example.demo to javafx.fxml, com.google.gson, com.fasterxml.jackson.databind;
    opens com.example.demo.controller to javafx.fxml, com.google.gson, com.fasterxml.jackson.databind;

    exports com.example.demo;
    exports com.example.demo.controller;
}
