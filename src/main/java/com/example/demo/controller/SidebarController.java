package com.example.demo.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class SidebarController {

    private DashboardFrameController mainController; 

    @FXML private Button btnDashboard;
    @FXML private Button btnAllFile;
    @FXML private Button btnShared;
    @FXML private Button btnInbox;
    @FXML private Button btnRecent;
    @FXML private Button btnStarred;
    @FXML private Button btnTrash;

    /** Nhận DashboardFrameController từ cha */
    public void setMainController(DashboardFrameController controller) {
        this.mainController = controller;
    }

    @FXML
    public void initialize() {

        btnDashboard.setOnAction(e ->
                mainController.loadContent("dashboard_content.fxml"));

        btnAllFile.setOnAction(e ->
                mainController.loadContent("allfile_content.fxml"));

        btnShared.setOnAction(e ->
                mainController.loadContent("shared_content.fxml"));

        btnInbox.setOnAction(e ->
                mainController.loadContent("inbox_content.fxml"));

        btnRecent.setOnAction(e ->
                mainController.loadContent("recent_content.fxml"));

        btnStarred.setOnAction(e ->
                mainController.loadContent("starred_content.fxml"));

        btnTrash.setOnAction(e ->
                mainController.loadContent("trash_content.fxml"));
    }
}