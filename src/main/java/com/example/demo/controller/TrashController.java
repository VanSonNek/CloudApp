package com.example.demo.controller;

import com.example.demo.ClientApiHandler;
import com.example.demo.ClientApiHandler.TrashResponse;
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
import javafx.util.Callback;

import java.util.List;
import java.util.concurrent.Executors;

public class TrashController {

    @FXML
    private ListView<TrashResponse> trashListView;

    private ObservableList<TrashResponse> trashList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Cấu hình cách hiển thị từng dòng trong ListView
        trashListView.setCellFactory(new Callback<ListView<TrashResponse>, ListCell<TrashResponse>>() {
            @Override
            public ListCell<TrashResponse> call(ListView<TrashResponse> param) {
                return new TrashListCell();
            }
        });

        loadTrashItems();
    }

    public void loadTrashItems() {
        // Clear danh sách cũ trước khi load để tránh duplicate nếu gọi nhiều lần
        Platform.runLater(() -> trashList.clear());

        Executors.newSingleThreadExecutor().execute(() -> {
            // Lấy dữ liệu từ Server
            List<TrashResponse> data = ClientApiHandler.getTrashItems();

            Platform.runLater(() -> {
                if (data != null) {
                    trashList.setAll(data);
                    trashListView.setItems(trashList);
                }
            });
        });
    }

    // --- CÁC HÀM XỬ LÝ SỰ KIỆN ---

    private void handleRestore(Long id, boolean isFolder) {
        Executors.newSingleThreadExecutor().execute(() -> {
            boolean success = ClientApiHandler.restoreItem(id, isFolder);

            Platform.runLater(() -> {
                if (success) {
                    showAlert("Thành công", "Đã khôi phục mục đã chọn.", Alert.AlertType.INFORMATION);
                    loadTrashItems(); // Load lại danh sách
                } else {
                    showAlert("Lỗi", "Khôi phục thất bại.", Alert.AlertType.ERROR);
                }
            });
        });
    }

    private void handleDeleteForever(Long trashId) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Hành động này sẽ XÓA VĨNH VIỄN dữ liệu khỏi hệ thống.\nKhông thể hoàn tác. Bạn chắc chắn chứ?",
                ButtonType.YES, ButtonType.NO);

        alert.setTitle("Xóa vĩnh viễn");
        alert.setHeaderText("Cảnh báo");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                Executors.newSingleThreadExecutor().execute(() -> {
                    // Gọi API xóa vĩnh viễn
                    boolean success = ClientApiHandler.deleteForever(trashId);

                    Platform.runLater(() -> {
                        if (success) {
                            loadTrashItems(); // Load lại danh sách ngay lập tức
                            showAlert("Thành công", "Đã xóa vĩnh viễn.", Alert.AlertType.INFORMATION);
                        } else {
                            showAlert("Lỗi", "Không thể xóa. Vui lòng thử lại.", Alert.AlertType.ERROR);
                        }
                    });
                });
            }
        });
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // --- INNER CLASS: Custom Cell (Giao diện từng dòng thùng rác) ---
    private class TrashListCell extends ListCell<TrashResponse> {
        @Override
        protected void updateItem(TrashResponse item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setGraphic(null);
                setText(null);
            } else {
                HBox hbox = new HBox(10);
                hbox.setAlignment(Pos.CENTER_LEFT);
                hbox.setStyle("-fx-padding: 10; -fx-border-color: #eee; -fx-border-width: 0 0 1 0;");

                // 1. Icon
                String iconType = item.isFolder ? "FOLDER" : "FILE";
                // Dùng IconHelper để lấy icon, nếu null thì bỏ qua
                ImageView icon = new ImageView(IconHelper.getFileIcon(iconType, item.itemName));
                icon.setFitWidth(32);
                icon.setFitHeight(32);

                // 2. Thông tin (Tên + Size)
                VBox infoBox = new VBox(2);
                Label nameLbl = new Label(item.itemName);
                nameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

                // ✅ FIX LỖI NULL POINTER TẠI ĐÂY
                String sizeText;
                if (item.isFolder) {
                    sizeText = "Thư mục";
                } else {
                    // Kiểm tra null an toàn cho item.size
                    long sizeVal = (item.size != null) ? item.size : 0L;
                    if (sizeVal < 1024) {
                        sizeText = sizeVal + " B";
                    } else {
                        sizeText = (sizeVal / 1024) + " KB";
                    }
                }

                // Xử lý ngày xóa (tránh null)
                String dateText = (item.deletedDate != null) ? item.deletedDate.toString() : "";

                Label detailLbl = new Label(sizeText + " • Đã xóa: " + dateText);
                detailLbl.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");

                infoBox.getChildren().addAll(nameLbl, detailLbl);

                // Spacer
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                // 3. Các nút bấm
                Button btnRestore = new Button("Khôi phục");
                btnRestore.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-cursor: hand;");
                btnRestore.setOnAction(e -> handleRestore(item.trashId, item.isFolder));

                Button btnDelete = new Button("Xóa vĩnh viễn");
                btnDelete.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand;");
                btnDelete.setOnAction(e -> handleDeleteForever(item.trashId));

                hbox.getChildren().addAll(icon, infoBox, spacer, btnRestore, btnDelete);
                setGraphic(hbox);
            }
        }
    }
}