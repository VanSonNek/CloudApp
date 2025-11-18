package com.example.demo.controller;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public class DashboardFrameController {

    @FXML private BorderPane rootFrame;  

    @FXML
    public void initialize() {
        loadSidebar();
        loadContent("dashboard_content.fxml");  // màn hình mặc định
    }

    /** Load file sidebar.fxml và truyền tham chiếu controller */
    private void loadSidebar() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/demo/sidebar.fxml") 
            );

            VBox sidebar = loader.load();
            SidebarController sidebarController = loader.getController();

            // LIÊN KẾT: Truyền tham chiếu cha vào SidebarController
            sidebarController.setMainController(this);

            rootFrame.setLeft(sidebar);

        } catch (IOException e) {
            System.err.println("Lỗi load Sidebar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Load nội dung vào vùng Center */
    public void loadContent(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/demo/" + fxmlFile)
            );

            rootFrame.setCenter(loader.load()); // Khi load allfile_content.fxml, nó sẽ gọi AllfileController.initialize()

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}