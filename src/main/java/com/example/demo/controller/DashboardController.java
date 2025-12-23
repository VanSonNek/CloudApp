package com.example.demo.controller;

import com.example.demo.ClientApiHandler;
import com.example.demo.ClientApiHandler.DashboardMetrics;
import com.example.demo.ListItem;
import com.example.demo.util.IconHelper;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class DashboardController {

    // ================== CẤU HÌNH ==================
    private static final long MAX_STORAGE = 10L * 1024 * 1024 * 1024; // 10 GB
    private static final long MAX_DAILY_TRANSFER = 5L * 1024 * 1024 * 1024; // 5 GB

    @FXML private StackPane storageCircle;
    @FXML private StackPane transferCircle;
    @FXML private ImageView avatarBtn;

    // Container hiển thị file (Dùng VBox thay vì ListView để giữ giao diện đẹp)
    @FXML private VBox recentFileContainer;

    @FXML private Label lblTotalUsage;
    @FXML private Label lblTransferUsage;
    @FXML private Region barActive;
    @FXML private Region barTrash;
    @FXML private Label lblActiveSize;
    @FXML private Label lblTrashSize;
    @FXML private Label lblAvailableText;

    // Ô tìm kiếm
    @FXML private TextField searchField;

    // Các nút lọc
    @FXML private Button btnAll, btnDoc, btnImg, btnMedia, btnOther;

    // Danh sách gốc chứa dữ liệu tải về
    private List<ListItem.FileDto> masterFileList = new ArrayList<>();

    // Biến lưu trạng thái lọc hiện tại (mặc định là ALL)
    private String currentFilterType = "ALL";

    @FXML
    public void initialize() {
        // 1. Tải số liệu biểu đồ
        loadDashboardData();

        // 2. Cài đặt sự kiện cho các nút lọc
        setupFilterButtons();

        // 3. Tải danh sách file
        loadRecentFiles();

        // 4. [SỬA LẠI] Sự kiện tìm kiếm: Kết hợp cả Tìm kiếm + Bộ lọc đang chọn
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                // Khi gõ phím, gọi lại hàm hiển thị với từ khóa mới
                refreshFileList(currentFilterType, newValue);
            });
        }
    }

    // ================== LOGIC TẢI & HIỂN THỊ FILE ==================

    private void loadRecentFiles() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Tải file từ server
                List<ListItem.FileDto> files = ClientApiHandler.getRecentFiles();

                Platform.runLater(() -> {
                    if (files != null) {
                        this.masterFileList = files; // Lưu vào kho gốc

                        // Mặc định hiển thị tab ALL và từ khóa rỗng
                        if (btnAll != null) applyFilter("ALL", btnAll);
                    }
                });
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    private void setupFilterButtons() {
        if (btnAll != null) btnAll.setOnAction(e -> applyFilter("ALL", btnAll));
        if (btnDoc != null) btnDoc.setOnAction(e -> applyFilter("DOC", btnDoc));
        if (btnImg != null) btnImg.setOnAction(e -> applyFilter("IMG", btnImg));
        if (btnMedia != null) btnMedia.setOnAction(e -> applyFilter("MEDIA", btnMedia));
        if (btnOther != null) btnOther.setOnAction(e -> applyFilter("OTHER", btnOther));
    }

    // Hàm xử lý khi bấm nút lọc (All, Doc...)
    private void applyFilter(String type, Button activeBtn) {
        this.currentFilterType = type; // Lưu lại loại đang chọn
        updateButtonStyle(activeBtn);  // Đổi màu nút

        // Lấy từ khóa hiện tại trong ô tìm kiếm (nếu có)
        String keyword = (searchField != null) ? searchField.getText() : "";

        // Gọi hàm refresh
        refreshFileList(type, keyword);
    }

    // [QUAN TRỌNG] Hàm trung tâm: Lọc theo LOẠI FILE + TỪ KHÓA TÌM KIẾM
    private void refreshFileList(String type, String keyword) {
        recentFileContainer.getChildren().clear();

        String lowerKeyword = (keyword == null) ? "" : keyword.trim().toLowerCase();

        // 1. Lọc dữ liệu từ masterFileList
        List<ListItem.FileDto> filesToShow = masterFileList.stream()
                // Điều kiện 1: Phải đúng loại (Doc, Img...) hoặc là ALL
                .filter(f -> "ALL".equals(type) || checkType(f.originalFilename, type))
                // Điều kiện 2: Tên file phải chứa từ khóa tìm kiếm
                .filter(f -> f.originalFilename.toLowerCase().contains(lowerKeyword))
                .collect(Collectors.toList());

        // 2. Hiển thị ra màn hình
        if (filesToShow.isEmpty()) {
            Label emptyLbl = new Label("Không tìm thấy file phù hợp.");
            emptyLbl.setStyle("-fx-text-fill: #999; -fx-padding: 20; -fx-font-style: italic;");
            recentFileContainer.getChildren().add(emptyLbl);
        } else {
            for (ListItem.FileDto f : filesToShow) {
                // Tái sử dụng hàm tạo giao diện dòng đẹp mắt của bạn
                recentFileContainer.getChildren().add(createFileRow(f));
            }
        }
    }

    // ... (Giữ nguyên các hàm checkType, updateButtonStyle, createFileRow) ...
    // ... Copy lại y nguyên các hàm logic cũ ở dưới đây ...

    private boolean checkType(String filename, String type) {
        if (filename == null) return false;
        String name = filename.toLowerCase();
        switch (type) {
            case "DOC":
                return name.endsWith(".doc") || name.endsWith(".docx") ||
                        name.endsWith(".pdf") || name.endsWith(".txt") ||
                        name.endsWith(".xls") || name.endsWith(".xlsx") ||
                        name.endsWith(".ppt") || name.endsWith(".pptx");
            case "IMG":
                return name.endsWith(".png") || name.endsWith(".jpg") ||
                        name.endsWith(".jpeg") || name.endsWith(".gif");
            case "MEDIA":
                return name.endsWith(".mp4") || name.endsWith(".mp3") ||
                        name.endsWith(".avi") || name.endsWith(".mkv") || name.endsWith(".wav");
            case "OTHER":
                return !checkType(filename, "DOC") && !checkType(filename, "IMG") && !checkType(filename, "MEDIA");
            default: return false;
        }
    }

    private void updateButtonStyle(Button activeBtn) {
        String defaultStyle = "-fx-background-color: #F0F2F5; -fx-text-fill: #666; -fx-background-radius: 20; -fx-cursor: hand;";
        String activeStyle = "-fx-background-color: #E7F3FF; -fx-text-fill: #2196F3; -fx-background-radius: 20; -fx-cursor: hand; -fx-font-weight: bold;";
        if (btnAll != null) btnAll.setStyle(defaultStyle);
        if (btnDoc != null) btnDoc.setStyle(defaultStyle);
        if (btnImg != null) btnImg.setStyle(defaultStyle);
        if (btnMedia != null) btnMedia.setStyle(defaultStyle);
        if (btnOther != null) btnOther.setStyle(defaultStyle);
        if (activeBtn != null) activeBtn.setStyle(activeStyle);
    }

    private HBox createFileRow(ListItem.FileDto file) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color:#FFFFFF; -fx-border-color:rgba(0,0,0,0.05); -fx-border-width:0 0 1 0;");
        row.setPadding(new Insets(12, 10, 12, 10));
        row.setCursor(Cursor.HAND);

        ImageView icon = new ImageView(IconHelper.getFileIcon("FILE", file.originalFilename));
        icon.setFitWidth(28); icon.setFitHeight(28);

        Label nameLabel = new Label(file.originalFilename);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #333;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label sizeLabel = new Label(formatSize(file.size));
        sizeLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 12px;");
        sizeLabel.setPrefWidth(80);
        sizeLabel.setAlignment(Pos.CENTER_RIGHT);

        row.getChildren().addAll(icon, nameLabel, spacer, sizeLabel);

        // Hover Effect
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color:#F9FAFB; -fx-border-color:rgba(0,0,0,0.05); -fx-border-width:0 0 1 0;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-background-color:#FFFFFF; -fx-border-color:rgba(0,0,0,0.05); -fx-border-width:0 0 1 0;"));

        return row;
    }

    // ================== LOGIC DASHBOARD BIỂU ĐỒ (Giữ nguyên) ==================

    private void loadDashboardData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            DashboardMetrics metrics = ClientApiHandler.getDashboardMetrics();
            Platform.runLater(() -> {
                if (metrics == null) return;
                long active = (metrics.activeSize != null) ? metrics.activeSize : 0;
                long trash = (metrics.trashSize != null) ? metrics.trashSize : 0;
                long transfer = (metrics.transferToday != null) ? metrics.transferToday : 0;
                double totalUsed = active + trash;
                double available = Math.max(0, MAX_STORAGE - totalUsed);

                double percentStorage = Math.min(100, (double) totalUsed / MAX_STORAGE * 100);
                createCircularProgress(storageCircle, percentStorage, Color.web("#333333"));
                lblTotalUsage.setText(formatSize((long) totalUsed) + " / 10 GB");

                double percentTransfer = Math.min(100, (double) transfer / MAX_DAILY_TRANSFER * 100);
                createCircularProgress(transferCircle, percentTransfer, Color.WHITE);
                lblTransferUsage.setText(formatSize(transfer) + " Uploaded");

                updateStorageBar(active, trash);
                lblActiveSize.setText(formatSize(active));
                lblTrashSize.setText(formatSize(trash));
                lblAvailableText.setText("Free: " + formatSize((long) available));
            });
        });
    }

    private void updateStorageBar(long active, long trash) {
        double totalWidth = 650.0;
        double widthActive = (double) active / MAX_STORAGE * totalWidth;
        double widthTrash = (double) trash / MAX_STORAGE * totalWidth;
        if (active > 0 && widthActive < 2) widthActive = 2;
        if (trash > 0 && widthTrash < 2) widthTrash = 2;

        double sum = widthActive + widthTrash;
        if (sum > totalWidth) {
            double scale = totalWidth / sum;
            widthActive *= scale;
            widthTrash *= scale;
        }
        barActive.setPrefWidth(widthActive);
        barTrash.setPrefWidth(widthTrash);
    }

    private String formatSize(Long size) {
        if (size == null) return "0 B";
        if (size < 1024) return size + " B";
        int z = (63 - Long.numberOfLeadingZeros(size)) / 10;
        return String.format("%.1f %sB", (double)size / (1L << (z*10)), " KMGTPE".charAt(z));
    }

    private void createCircularProgress(StackPane stackPane, double percent, Color color) {
        stackPane.getChildren().clear();
        stackPane.setPrefSize(80, 80);
        Arc bgArc = new Arc(0, 0, 35, 35, 90, 360);
        bgArc.setType(ArcType.OPEN);
        bgArc.setStroke(Color.rgb(255, 255, 255, 0.4));
        bgArc.setStrokeWidth(8);
        bgArc.setFill(Color.TRANSPARENT);

        Arc progArc = new Arc(0, 0, 35, 35, 90, 0);
        progArc.setType(ArcType.OPEN);
        progArc.setStroke(color);
        progArc.setStrokeWidth(8);
        progArc.setStrokeLineCap(StrokeLineCap.ROUND);
        progArc.setFill(Color.TRANSPARENT);

        Label lbl = new Label((int) percent + "%");
        if(color.equals(Color.WHITE)) lbl.setTextFill(Color.WHITE);
        else lbl.setTextFill(Color.web("#333"));
        lbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        stackPane.getChildren().addAll(new javafx.scene.Group(bgArc, progArc), lbl);

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, e -> { progArc.setLength(0); lbl.setText("0%"); }),
                new KeyFrame(Duration.seconds(1), e -> {
                    progArc.setLength(-(percent * 3.6));
                    lbl.setText((int) percent + "%");
                })
        );
        timeline.play();
    }
}