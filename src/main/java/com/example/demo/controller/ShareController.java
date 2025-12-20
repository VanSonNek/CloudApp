package com.example.demo.controller;

import com.example.demo.ClientApiHandler;
import com.example.demo.ClientApiHandler.ShareResponse;
import com.example.demo.util.IconHelper;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Callback;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.Executors;

public class ShareController {

    @FXML
    private ListView<ShareResponse> shareListView;

    private ObservableList<ShareResponse> shareList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Cấu hình giao diện cho từng dòng trong danh sách
        shareListView.setCellFactory(new Callback<ListView<ShareResponse>, ListCell<ShareResponse>>() {
            @Override
            public ListCell<ShareResponse> call(ListView<ShareResponse> param) {
                return new ShareListCell();
            }
        });

        loadSharedFiles();
    }

    private void loadSharedFiles() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<ShareResponse> data = ClientApiHandler.getSharedFiles();
            Platform.runLater(() -> {
                if (data != null) {
                    shareList.setAll(data);
                    shareListView.setItems(shareList);
                }
            });
        });
    }

    // --- LOGIC DOWNLOAD FILE ĐƯỢC CHIA SẺ ---
    private void handleDownload(ShareResponse item) {
        if (item.isFolder) {
            showAlert("Thông báo", "Hiện chưa hỗ trợ tải cả thư mục được chia sẻ.", Alert.AlertType.INFORMATION);
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setInitialFileName(item.fileName);
        fc.setTitle("Lưu file được chia sẻ");
        File dest = fc.showSaveDialog(shareListView.getScene().getWindow());

        if (dest != null) {
            Executors.newSingleThreadExecutor().execute(() -> {
                // Dùng fileId để tải
                File temp = ClientApiHandler.downloadFileToTemp(item.fileId, item.fileName);

                Platform.runLater(() -> {
                    if (temp != null) {
                        try {
                            Files.copy(temp.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            showAlert("Thành công", "Đã tải file về máy.", Alert.AlertType.INFORMATION);
                        } catch (Exception e) {
                            showAlert("Lỗi", "Không thể lưu file: " + e.getMessage(), Alert.AlertType.ERROR);
                        }
                    } else {
                        showAlert("Lỗi", "Tải file thất bại từ server.", Alert.AlertType.ERROR);
                    }
                });
            });
        }
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // --- INNER CLASS: Giao diện từng dòng (ListCell) ---
    private class ShareListCell extends ListCell<ShareResponse> {
        @Override
        protected void updateItem(ShareResponse item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setGraphic(null);
                setText(null);
            } else {
                HBox hbox = new HBox(10);
                hbox.setAlignment(Pos.CENTER_LEFT);
                hbox.setStyle("-fx-padding: 10; -fx-border-color: #eee; -fx-border-width: 0 0 1 0;");

                // 1. Xử lý Icon & Loại item (Sửa lỗi itemType)
                String typeStr = item.isFolder ? "FOLDER" : "FILE";
                ImageView icon = new ImageView(IconHelper.getFileIcon(typeStr, item.fileName));
                icon.setFitWidth(32);
                icon.setFitHeight(32);

                // 2. Thông tin (Sửa lỗi itemName, senderEmail)
                VBox infoBox = new VBox(2);

                // itemName -> item.fileName
                Label nameLbl = new Label(item.fileName);
                nameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

                // senderEmail -> item.ownerEmail
                Label ownerLbl = new Label("Người gửi: " + item.ownerEmail + " • Ngày: " + item.sharedDate);
                ownerLbl.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");

                infoBox.getChildren().addAll(nameLbl, ownerLbl);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                // 3. Nút Download (Sửa lỗi itemId)
                Button btnDownload = new Button("Tải xuống");
                btnDownload.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand;");
                btnDownload.setOnAction(e -> handleDownload(item));

                hbox.getChildren().addAll(icon, infoBox, spacer, btnDownload);
                setGraphic(hbox);
            }
        }
    }
}