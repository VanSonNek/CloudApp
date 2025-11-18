package com.example.demo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.IOException;

public class Login extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("login.fxml"));
        VBox root = fxmlLoader.load();

        // Set background image full-cloud từ resources
        root.setStyle(
                "-fx-background-image: url('" + getClass().getResource("/com/example/demo/imgs/fullcloud2.jpg" +
                        "").toExternalForm() + "');" +
                        "-fx-background-size: cover;" +
                        "-fx-background-position: center center;" +
                        "-fx-background-repeat: no-repeat;"
        );

        Scene scene = new Scene(root, 1200, 750);
        stage.setTitle("Login");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
