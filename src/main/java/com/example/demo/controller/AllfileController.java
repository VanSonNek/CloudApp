package com.example.demo.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

import java.util.Arrays;
import java.util.List;

public class AllfileController {

    @FXML private Button btnDashboard;
    @FXML private Button btnAllFile;
    @FXML private Button btnShared;
    @FXML private Button btnInbox;
    @FXML private Button btnRecent;
    @FXML private Button btnStarred;
    @FXML private Button btnTrash;

    private List<Button> sidebarButtons;

    @FXML
    public void initialize() {

        // Danh sách nút sidebar
        sidebarButtons = Arrays.asList(
                btnDashboard, btnAllFile, btnShared,
                btnInbox, btnRecent, btnStarred, btnTrash
        );

        // Gán sự kiện click cho từng button
        sidebarButtons.forEach(btn ->
                btn.setOnAction(e -> setActiveSidebarButton(btn))
        );

        // Mặc định chọn All File
        setActiveSidebarButton(btnAllFile);
    }

    /** Đổi highlight menu đang chọn */
    private void setActiveSidebarButton(Button activeBtn) {
        sidebarButtons.forEach(btn ->
                btn.getStyleClass().remove("sidebar-button-active")
        );

        if (!activeBtn.getStyleClass().contains("sidebar-button-active")) {
            activeBtn.getStyleClass().add("sidebar-button-active");
        }
    }
}
