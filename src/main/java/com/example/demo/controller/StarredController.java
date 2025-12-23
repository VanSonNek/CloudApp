package com.example.demo.controller;

import com.example.demo.ClientApiHandler;
import com.example.demo.ListItem;
import com.example.demo.util.IconHelper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*; // Đã bao gồm TextField
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors; // Cần thêm thư viện này để lọc

public class StarredController {

    @FXML private ListView<ListItem.FileDto> fileListView;

    // 1. [MỚI] Khai báo ô tìm kiếm (khớp với fx:id bên FXML)
    @FXML private TextField searchField;

    // 2. [MỚI] Danh sách gốc để lưu trữ dữ liệu tải từ server
    private ObservableList<ListItem.FileDto> masterData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Cấu hình hiển thị Cell
        fileListView.setCellFactory(param -> new StarredFileCell());

        // 3. [MỚI] Lắng nghe sự kiện gõ phím để lọc danh sách ngay lập tức
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                filterList(newValue);
            });
        }

        loadData();
    }

    private void loadData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            // Lấy dữ liệu từ Server
            List<ListItem.FileDto> files = ClientApiHandler.getStarredFiles();

            Platform.runLater(() -> {
                if (files != null) {
                    // 4. [QUAN TRỌNG] Lưu vào danh sách gốc trước
                    masterData.setAll(files);
                    // Hiển thị ra màn hình
                    fileListView.setItems(masterData);
                }
            });
        });
    }

    // 5. [MỚI] Hàm xử lý logic tìm kiếm
    private void filterList(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            // Nếu ô tìm kiếm trống -> Hiện lại toàn bộ từ danh sách gốc
            fileListView.setItems(masterData);
        } else {
            // Lọc theo tên file (không phân biệt hoa thường)
            String lowerCaseFilter = keyword.toLowerCase();
            List<ListItem.FileDto> filteredList = masterData.stream()
                    .filter(file -> file.originalFilename.toLowerCase().contains(lowerCaseFilter))
                    .collect(Collectors.toList());

            // Cập nhật giao diện với danh sách đã lọc
            fileListView.setItems(FXCollections.observableArrayList(filteredList));
        }
    }

    // Class nội bộ để hiển thị giao diện từng dòng (Giữ nguyên logic cũ)
    private class StarredFileCell extends ListCell<ListItem.FileDto> {
        @Override
        protected void updateItem(ListItem.FileDto item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                GridPane grid = new GridPane();
                grid.setHgap(10);
                grid.setAlignment(Pos.CENTER_LEFT);

                // Cột 1: Icon + Tên
                HBox nameBox = new HBox(10);
                nameBox.setAlignment(Pos.CENTER_LEFT);
                ImageView icon = new ImageView(IconHelper.getFileIcon("FILE", item.originalFilename));
                icon.setFitWidth(24); icon.setFitHeight(24);
                Label nameLabel = new Label(item.originalFilename);
                nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                nameBox.getChildren().addAll(icon, nameLabel);

                // Cột 2: Người sở hữu (Mockup)
                Label ownerLabel = new Label("Me");

                // Cột 3: Kích thước
                String sizeStr = (item.size != null) ? (item.size / 1024) + " KB" : "0 KB";
                Label sizeLabel = new Label(sizeStr);

                // Cột 4: Nút Star (Bỏ thích)
                ToggleButton btnStar = new ToggleButton("★");
                btnStar.setSelected(true);
                btnStar.setStyle("-fx-background-color: transparent; -fx-text-fill: #f1c40f; -fx-font-size: 16px; -fx-cursor: hand;");

                btnStar.setOnAction(e -> {
                    // [QUAN TRỌNG] Xóa khỏi cả 2 danh sách để đồng bộ
                    getListView().getItems().remove(item);
                    masterData.remove(item);

                    Executors.newSingleThreadExecutor().execute(() -> {
                        ClientApiHandler.toggleStar(item.id, false, false);
                    });
                });

                grid.getColumnConstraints().addAll(
                        new ColumnConstraints(550),
                        new ColumnConstraints(140),
                        new ColumnConstraints(100),
                        new ColumnConstraints(120)
                );

                grid.add(nameBox, 0, 0);
                grid.add(ownerLabel, 1, 0);
                grid.add(sizeLabel, 2, 0);
                grid.add(btnStar, 3, 0);

                setGraphic(grid);
            }
        }
    }
}