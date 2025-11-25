package com.example.demo.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class SidebarController {

    private DashboardFrameController mainController; 

    @FXML private Button btnDashboard;
    @FXML private Button btnAllFile;
    @FXML private Button btnShared;
    @FXML private Button btnInbox;
    @FXML private Button btnAccount;
    @FXML private Button btnStarred;
    @FXML private Button btnTrash;

    /** Nhận DashboardFrameController từ cha */
    public void setMainController(DashboardFrameController controller) {
        this.mainController = controller;
    }

    @FXML
    public void initialize() {

        btnDashboard.setOnAction(e -> {
            highlight(btnDashboard);
            mainController.loadContent("dashboard_content.fxml");
        });

        btnAllFile.setOnAction(e -> {
            highlight(btnAllFile);
            mainController.loadContent("allfile_content.fxml");
        });

        btnShared.setOnAction(e -> {
            highlight(btnShared);
            mainController.loadContent("share_content.fxml");
        });

        btnInbox.setOnAction(e -> {
            highlight(btnInbox);
            mainController.loadContent("inbox_content.fxml");
        });

        btnAccount.setOnAction(e -> {
            highlight(btnAccount);
            mainController.loadContent("account_content.fxml");
        });

        btnStarred.setOnAction(e -> {
            highlight(btnStarred);
            mainController.loadContent("starred_content.fxml");
        });

        btnTrash.setOnAction(e -> {
            highlight(btnTrash);
            mainController.loadContent("trash_content.fxml");
        });

        // Mặc định Dashboard active khi mở app
        highlight(btnDashboard);
    }

    private void highlight(Button activeBtn) {
        // Xóa active ở tất cả nút
        btnDashboard.getStyleClass().remove("sidebar-btn-active");
        btnAllFile.getStyleClass().remove("sidebar-btn-active");
        btnShared.getStyleClass().remove("sidebar-btn-active");
        btnInbox.getStyleClass().remove("sidebar-btn-active");
        btnAccount.getStyleClass().remove("sidebar-btn-active");
        btnStarred.getStyleClass().remove("sidebar-btn-active");
        btnTrash.getStyleClass().remove("sidebar-btn-active");

        // Thêm active vào nút được click
        if (!activeBtn.getStyleClass().contains("sidebar-btn-active")) {
            activeBtn.getStyleClass().add("sidebar-btn-active");
        }
    }

}