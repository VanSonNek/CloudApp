package com.example.demo.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class DashboardFrameController {

    @FXML private BorderPane rootFrame;  // BorderPane chính trong DashboardFrame.fxml

    @FXML
    public void initialize() {
        loadSidebar();
        loadContent("dashboard_content.fxml");  // màn hình mặc định
    }

    /** Load file sidebar.fxml */
    private void loadSidebar() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/demo/sidebar.fxml")
            );

            VBox sidebar = loader.load();

            // Lấy controller của sidebar
            SidebarController sidebarController = loader.getController();

            // Truyền tham chiếu cha vào SidebarController
            sidebarController.setMainController(this);

            // Đặt sidebar vào bên trái
            rootFrame.setLeft(sidebar);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Load nội dung vào vùng Center */
    public void loadContent(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/demo/" + fxmlFile)
            );

            rootFrame.setCenter(loader.load());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
