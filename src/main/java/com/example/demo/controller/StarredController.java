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

    private class StarredFileCell extends ListCell<ListItem.FileDto> {
        @Override
        protected void updateItem(ListItem.FileDto item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                setStyle("-fx-background-color: transparent;"); // Ẩn các ô trống
            } else {
                // 1. Tạo Container chính (GridPane) cho row
                GridPane grid = new GridPane();
                grid.getStyleClass().add("file-card"); // Áp dụng CSS class "file-card"
                grid.setMinHeight(60); // Chiều cao cố định cho đẹp
                grid.setAlignment(Pos.CENTER_LEFT);

                // 2. Thiết lập cột (Phải khớp % với Header bên FXML)
                ColumnConstraints col1 = new ColumnConstraints(); col1.setPercentWidth(50);
                ColumnConstraints col2 = new ColumnConstraints(); col2.setPercentWidth(20);
                ColumnConstraints col3 = new ColumnConstraints(); col3.setPercentWidth(15);
                ColumnConstraints col4 = new ColumnConstraints(); col4.setPercentWidth(15); col4.setHalignment(javafx.geometry.HPos.CENTER);

                grid.getColumnConstraints().addAll(col1, col2, col3, col4);

                // --- CỘT 1: ICON + TÊN FILE ---
                HBox nameBox = new HBox(15);
                nameBox.setAlignment(Pos.CENTER_LEFT);

                // Icon (Nên dùng ảnh SVG hoặc ảnh chất lượng cao)
                ImageView icon = new ImageView(IconHelper.getFileIcon("FILE", item.originalFilename));
                icon.setFitWidth(32);
                icon.setFitHeight(32);

                // Tên file
                Label nameLabel = new Label(item.originalFilename);
                nameLabel.getStyleClass().add("file-name"); // CSS class

                nameBox.getChildren().addAll(icon, nameLabel);

                // --- CỘT 2: OWNER ---
                Label ownerLabel = new Label("Me"); // Mockup
                ownerLabel.getStyleClass().add("file-info");

                // --- CỘT 3: SIZE ---
                String sizeStr = (item.size != null) ? (item.size / 1024) + " KB" : "0 KB";
                Label sizeLabel = new Label(sizeStr);
                sizeLabel.getStyleClass().add("file-info");

                // --- CỘT 4: NÚT STAR ---
                ToggleButton btnStar = new ToggleButton("★");
                btnStar.setSelected(true);
                btnStar.getStyleClass().add("star-button"); // CSS class

                // Xử lý sự kiện click
                btnStar.setOnAction(e -> {
                    // Xóa khỏi ListView ngay lập tức để tạo cảm giác mượt mà
                    getListView().getItems().remove(item);
                    masterData.remove(item);

                    // Gọi API ngầm
                    Executors.newSingleThreadExecutor().execute(() -> {
                        ClientApiHandler.toggleStar(item.id, false, false);
                    });
                });

                // 3. Add vào Grid
                grid.add(nameBox, 0, 0);
                grid.add(ownerLabel, 1, 0);
                grid.add(sizeLabel, 2, 0);
                grid.add(btnStar, 3, 0);

                setGraphic(grid);
                setText(null);
            }
        }}
}