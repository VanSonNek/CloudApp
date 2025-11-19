package com.example.demo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Register extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader loader = new FXMLLoader(getClass().getResource("register.fxml"));
        VBox root = loader.load();

        // Set background image giống Login
        root.setStyle(
                "-fx-background-image: url('"
                        + getClass().getResource("/com/example/demo/imgs/fullcloud2.jpg").toExternalForm() + "');" +
                        "-fx-background-size: cover;" +
                        "-fx-background-position: center center;" +
                        "-fx-background-repeat: no-repeat;"
        );

        Scene scene = new Scene(root, 1200, 750);
        stage.setTitle("Create Account");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
