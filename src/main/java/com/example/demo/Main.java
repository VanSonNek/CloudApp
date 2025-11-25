package com.example.demo;

import java.io.IOException;

import com.example.demo.controller.LoginController;
import com.example.demo.controller.RegisterController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Main extends Application {

    private Stage primaryStage;

    private static final String LOGIN_FXML = "/com/example/demo/login.fxml";
    private static final String REGISTER_FXML = "/com/example/demo/register.fxml";
    private static final String DASHBOARD_FRAME_FXML = "/com/example/demo/DashboardFrame.fxml";

    @Override
    public void start(Stage stage) throws Exception {
        this.primaryStage = stage;

        // Mặc định mở trang login
//        showLoginScene();
        showDashboardScene();
    }

    // ==================== LOGIN ====================
    public void showLoginScene() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(LOGIN_FXML));
        VBox loginRoot = loader.load();

        // Gắn Main vào Controller để có thể chuyển scene
        LoginController loginController = loader.getController();
        loginController.setMainApp(this);

        // Background
        loginRoot.setStyle(
                "-fx-background-image: url('" +
                        getClass().getResource("/com/example/demo/imgs/fullcloud2.jpg").toExternalForm() +
                        "');" +
                        "-fx-background-size: cover;" +
                        "-fx-background-position: center;" +
                        "-fx-background-repeat: no-repeat;"
        );

        Scene scene = new Scene(loginRoot, 1200, 750);
        primaryStage.setTitle("Skybox Login");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // ================= REGISTER =====================
    public void showRegisterScene() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(REGISTER_FXML));
            VBox root = loader.load();

            RegisterController regController = loader.getController();
            regController.setMainApp(this);

            // Background
            root.setStyle(
                    "-fx-background-image: url('" +
                            getClass().getResource("/com/example/demo/imgs/fullcloud2.jpg").toExternalForm() +
                            "');" +
                            "-fx-background-size: cover;" +
                            "-fx-background-position: center;" +
                            "-fx-background-repeat: no-repeat;"
            );

            primaryStage.setScene(new Scene(root, 1200, 750));
            primaryStage.setTitle("Skybox - Create Account");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= DASHBOARD =====================
    public void showDashboardScene() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(DASHBOARD_FRAME_FXML)
            );

            BorderPane dashboardRoot = loader.load();

            Scene scene = new Scene(dashboardRoot, 1400, 900);
            primaryStage.setTitle("Skybox Dashboard");
            primaryStage.setScene(scene);
            primaryStage.show();

        } catch (IOException e) {
            System.err.println("❌ Lỗi khi load Dashboard Frame: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
