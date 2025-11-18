package com.example.demo;

import java.io.IOException;

import com.example.demo.controller.LoginController;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox; 
import javafx.stage.Stage;

public class Main extends Application {

    private Stage primaryStage; 
    private static final String LOGIN_FXML = "login.fxml"; 
    private static final String DASHBOARD_FRAME_FXML = "DashboardFrame.fxml"; 

    @Override
    public void start(Stage stage) throws Exception {
        this.primaryStage = stage;
        showLoginScene(); // Bắt đầu bằng màn hình Login
    }
    
    /**
     * Hiển thị màn hình Đăng nhập và thiết lập liên kết ngược (Main <-> LoginController).
     */
    public void showLoginScene() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(LOGIN_FXML));
        VBox loginRoot = loader.load();
        
        // <<< LIÊN KẾT CHÍNH: Truyền tham chiếu Main App cho LoginController
        LoginController loginController = loader.getController();
        loginController.setMainApp(this); 
        
        // (Thêm code styling background cũ vào đây nếu cần, ví dụ:)
        
        loginRoot.setStyle(
                "-fx-background-image: url('" + getClass().getResource("/com/example/demo/imgs/fullcloud2.jpg").toExternalForm() + "');" +
                "-fx-background-size: cover;" +
                "-fx-background-position: center center;" +
                "-fx-background-repeat: no-repeat;"
        );
        

        Scene scene = new Scene(loginRoot, 1200, 750);
        primaryStage.setTitle("Skybox Login");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Chuyển sang màn hình Dashboard sau khi đăng nhập thành công.
     */
    public void showDashboardScene() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(DASHBOARD_FRAME_FXML) 
            );

            BorderPane dashboardRoot = loader.load(); 
            
            Scene scene = new Scene(dashboardRoot, 1400, 900); 
            primaryStage.setTitle("Skybox Dashboard");
            primaryStage.setScene(scene);
            
        } catch (IOException e) {
            System.err.println("❌ Lỗi khi load Dashboard Frame: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}