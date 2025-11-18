package com.example.demo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Allfile extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("allfile.fxml"));
        Scene scene = new Scene(loader.load());
        scene.getStylesheets().add(getClass().getResource("sidebar.css").toExternalForm());
        stage.setScene(scene);
        stage.setTitle("Skybox AllFile");
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
