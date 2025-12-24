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
import javafx.scene.layout.*;
import javafx.util.Callback;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executors;

public class TrashController {

    @FXML
    private ListView<TrashResponse> trashListView;

    @FXML
    private Button btnDeleteAll; // Khai báo thêm button này để tránh lỗi FXML injection nếu cần dùng sau này

    private ObservableList<TrashResponse> trashList = FXCollections.observableArrayList();

    // Formatter để hiển thị ngày tháng đẹp hơn (UI logic)
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        // Cấu hình cách hiển thị từng dòng trong ListView dùng Cell Factory mới
        trashListView.setCellFactory(new Callback<ListView<TrashResponse>, ListCell<TrashResponse>>() {
            @Override
            public ListCell<TrashResponse> call(ListView<TrashResponse> param) {
                return new TrashListCell();
            }
        });

        if (btnDeleteAll != null) {
            btnDeleteAll.setOnAction(e -> handleDeleteAll());
        }

        loadTrashItems();
    }

    public void loadTrashItems() {
        Platform.runLater(() -> trashList.clear());

        Executors.newSingleThreadExecutor().execute(() -> {
            List<TrashResponse> data = ClientApiHandler.getTrashItems();
            Platform.runLater(() -> {
                if (data != null) {
                    trashList.setAll(data);
                    trashListView.setItems(trashList);
                }
            });
        });
    }

    // --- CÁC HÀM XỬ LÝ SỰ KIỆN (GIỮ NGUYÊN LOGIC CŨ) ---

    private void handleRestore(Long id, boolean isFolder) {
        Executors.newSingleThreadExecutor().execute(() -> {
            boolean success = ClientApiHandler.restoreItem(id, isFolder);

            Platform.runLater(() -> {
                if (success) {
                    showAlert("Thành công", "Đã khôi phục mục đã chọn.", Alert.AlertType.INFORMATION);
                    loadTrashItems();
                } else {
                    showAlert("Lỗi", "Khôi phục thất bại.", Alert.AlertType.ERROR);
                }
            });
        });
    }

    private void handleDeleteAll() {
        if (trashList.isEmpty()) {
            showAlert("Thông báo", "Thùng rác đang trống.", Alert.AlertType.INFORMATION);
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Bạn có chắc muốn làm sạch thùng rác? Tất cả file sẽ mất vĩnh viễn.",
                ButtonType.YES, ButtonType.NO);
        alert.setTitle("Xóa tất cả");
        alert.setHeaderText("Cảnh báo nguy hiểm");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                Executors.newSingleThreadExecutor().execute(() -> {
                    // Gọi API xóa tất cả
                    boolean success = ClientApiHandler.deleteAllTrash();

                    Platform.runLater(() -> {
                        if (success) {
                            loadTrashItems(); // Load lại danh sách (sẽ trống)
                            showAlert("Thành công", "Đã dọn sạch thùng rác.", Alert.AlertType.INFORMATION);
                        } else {
                            showAlert("Lỗi", "Không thể dọn sạch thùng rác. Vui lòng thử lại.", Alert.AlertType.ERROR);
                        }
                    });
                });
            }
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
                    boolean success = ClientApiHandler.deleteForever(trashId);

                    Platform.runLater(() -> {
                        if (success) {
                            loadTrashItems();
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

    // --- INNER CLASS: Custom Cell (Giao diện MỚI đẹp hơn) ---
    // --- INNER CLASS: Custom Cell (Đã sửa lỗi format ngày) ---
    private class TrashListCell extends ListCell<TrashResponse> {
        @Override
        protected void updateItem(TrashResponse item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                setStyle("-fx-background-color: transparent; -fx-padding: 0;");
            } else {
                GridPane grid = new GridPane();
                grid.setAlignment(Pos.CENTER_LEFT);
                grid.setStyle("-fx-background-color: white; -fx-padding: 8 15; -fx-border-color: #f0f0f0; -fx-border-width: 0 0 1 0;");

                // Fix lỗi hiển thị cột bị mất
                if (getListView() != null) {
                    grid.prefWidthProperty().bind(getListView().widthProperty().subtract(30));
                }

                // Cấu hình cột
                ColumnConstraints colName = new ColumnConstraints(); colName.setPercentWidth(45);
                ColumnConstraints colDate = new ColumnConstraints(); colDate.setPercentWidth(20); colDate.setHalignment(javafx.geometry.HPos.CENTER);
                ColumnConstraints colSize = new ColumnConstraints(); colSize.setPercentWidth(15); colSize.setHalignment(javafx.geometry.HPos.CENTER);
                ColumnConstraints colAction = new ColumnConstraints(); colAction.setPercentWidth(20); colAction.setHalignment(javafx.geometry.HPos.RIGHT);

                grid.getColumnConstraints().addAll(colName, colDate, colSize, colAction);

                // --- 1. CỘT TÊN ---
                HBox nameBox = new HBox(12);
                nameBox.setAlignment(Pos.CENTER_LEFT);

                String iconType = item.isFolder ? "FOLDER" : "FILE";
                ImageView icon = new ImageView(IconHelper.getFileIcon(iconType, item.itemName));
                icon.setFitWidth(24);
                icon.setFitHeight(24);

                Label nameLbl = new Label(item.itemName != null ? item.itemName : "Không tên");
                nameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #333;");
                nameBox.getChildren().addAll(icon, nameLbl);

                // --- 2. CỘT NGÀY XÓA (FIX LỖI TẠI ĐÂY) ---
                String dateText = "-";
                if (item.deletedDate != null) {
                    String rawDate = item.deletedDate.toString(); // Lấy chuỗi gốc
                    try {
                        // Thử chuyển đổi chuỗi sang dạng Date để format đẹp hơn
                        // Lưu ý: Cần import java.time.LocalDateTime
                        LocalDateTime ldt = LocalDateTime.parse(rawDate);
                        dateText = ldt.format(DATE_FORMATTER);
                    } catch (Exception e) {
                        // Nếu lỗi (do format khác chuẩn ISO) thì hiển thị nguyên gốc
                        dateText = rawDate;
                    }
                }
                Label dateLbl = new Label(dateText);
                dateLbl.setStyle("-fx-text-fill: #666; -fx-font-size: 13px;");

                // --- 3. CỘT KÍCH THƯỚC ---
                String sizeText;
                if (item.isFolder) {
                    sizeText = "-";
                } else {
                    long sizeVal = (item.size != null) ? item.size : 0L;
                    if (sizeVal < 1024) {
                        sizeText = sizeVal + " B";
                    } else if (sizeVal < 1024 * 1024) {
                        sizeText = String.format("%.1f KB", sizeVal / 1024.0);
                    } else {
                        sizeText = String.format("%.1f MB", sizeVal / (1024.0 * 1024.0));
                    }
                }
                Label sizeLbl = new Label(sizeText);
                sizeLbl.setStyle("-fx-text-fill: #666; -fx-font-size: 13px;");

                // --- 4. CỘT HÀNH ĐỘNG ---
                HBox actionBox = new HBox(8);
                actionBox.setAlignment(Pos.CENTER_RIGHT);

                Button btnRestore = new Button("Khôi phục");
                btnRestore.setStyle("-fx-background-color: transparent; -fx-border-color: #2ecc71; -fx-border-radius: 4; -fx-text-fill: #2ecc71; -fx-cursor: hand; -fx-font-size: 11px;");
                btnRestore.setOnAction(e -> handleRestore(item.trashId, item.isFolder));

                Button btnDelete = new Button("Xóa");
                btnDelete.setStyle("-fx-background-color: transparent; -fx-border-color: #e74c3c; -fx-border-radius: 4; -fx-text-fill: #e74c3c; -fx-cursor: hand; -fx-font-size: 11px;");
                btnDelete.setOnAction(e -> handleDeleteForever(item.trashId));

                actionBox.getChildren().addAll(btnRestore, btnDelete);

                // Add vào Grid
                grid.add(nameBox, 0, 0);
                grid.add(dateLbl, 1, 0);
                grid.add(sizeLbl, 2, 0);
                grid.add(actionBox, 3, 0);

                setGraphic(grid);
            }
        }
    }
}