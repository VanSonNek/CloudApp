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
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;

import java.util.List;
import java.util.concurrent.Executors;

public class DashboardController {

    // ================== CẤU HÌNH ==================
    private static final long MAX_STORAGE = 10L * 1024 * 1024 * 1024; // 10 GB
    private static final long MAX_DAILY_TRANSFER = 5L * 1024 * 1024 * 1024; // 5 GB (Limit upload/ngày)

    @FXML private StackPane storageCircle;
    @FXML private StackPane transferCircle;
    @FXML private ImageView avatarBtn;
    @FXML private VBox recentFileContainer;

    // Các biến hiển thị số liệu
    @FXML private Label lblTotalUsage;
    @FXML private Label lblTransferUsage;

    // Các biến cho thanh 3 màu (Active - Trash - Free)
    @FXML private Region barActive;
    @FXML private Region barTrash;
    @FXML private Label lblActiveSize;
    @FXML private Label lblTrashSize;
    @FXML private Label lblAvailableText;

    @FXML
    public void initialize() {
        // 1. Tải số liệu Dashboard
        loadDashboardData();

        // 2. Tải danh sách file gần đây
        loadRecentFiles();
    }

    private void loadDashboardData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            // Lấy dữ liệu từ Server
            DashboardMetrics metrics = ClientApiHandler.getDashboardMetrics();

            Platform.runLater(() -> {
                // ✅ FIX 1: Kiểm tra null nếu không gọi được API
                if (metrics == null) {
                    System.out.println("Không lấy được dữ liệu Metrics.");
                    return;
                }

                // Lấy giá trị an toàn
                long active = (metrics.activeSize != null) ? metrics.activeSize : 0;
                long trash = (metrics.trashSize != null) ? metrics.trashSize : 0;
                long transfer = (metrics.transferToday != null) ? metrics.transferToday : 0;

                double totalUsed = active + trash;

                double available = MAX_STORAGE - totalUsed;
                if (available < 0) available = 0;

                // --- A. CẬP NHẬT CARD STORAGE (Màu Vàng) ---
                double percentStorage = (double) totalUsed / MAX_STORAGE * 100;System.out.println(percentStorage);
                if (percentStorage > 100) percentStorage = 100; // Cap max 100%

                createCircularProgress(storageCircle, percentStorage, Color.WHITE);
                lblTotalUsage.setText(formatSize((long) totalUsed) + " / 10 GB");

                // --- B. CẬP NHẬT CARD TRANSFER (Màu Tím) ---
                double percentTransfer = (double) transfer / MAX_DAILY_TRANSFER * 100;
                if (percentTransfer > 100) percentTransfer = 100;

                createCircularProgress(transferCircle, percentTransfer, Color.WHITE);
                lblTransferUsage.setText(formatSize(transfer) + " Uploaded");

                // --- C. CẬP NHẬT THANH 3 MÀU ---
                updateStorageBar(active, trash);

                // Cập nhật text chi tiết
                lblActiveSize.setText(formatSize(active));
                lblTrashSize.setText(formatSize(trash));
                lblAvailableText.setText("Free: " + formatSize((long) available));
            });
        });
    }

    private void updateStorageBar(long active, long trash) {
        // Tổng chiều rộng thanh bar là 650px (theo FXML)
        double totalWidth = 650.0;

        double widthActive = (double) active / MAX_STORAGE * totalWidth;
        double widthTrash = (double) trash / MAX_STORAGE * totalWidth;

        // ✅ FIX 2: Nếu file có nhưng quá nhỏ (<2px), hiển thị tối thiểu 2px để người dùng thấy
        if (active > 0 && widthActive < 2) widthActive = 2;
        if (trash > 0 && widthTrash < 2) widthTrash = 2;

        // Giới hạn không cho tràn thanh nếu vượt quá 10GB
        if (widthActive + widthTrash > totalWidth) {
            double scale = totalWidth / (widthActive + widthTrash);
            widthActive *= scale;
            widthTrash *= scale;
        }

        barActive.setPrefWidth(widthActive);
        barTrash.setPrefWidth(widthTrash);
    }

    private void loadRecentFiles() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<ListItem.FileDto> files = ClientApiHandler.getRecentFiles();
                Platform.runLater(() -> {
                    recentFileContainer.getChildren().clear();
                    // ✅ FIX 3: Check null trước khi check isEmpty
                    if (files == null || files.isEmpty()) {
                        Label emptyLbl = new Label("Chưa có file nào.");
                        emptyLbl.setStyle("-fx-text-fill: #888; -fx-padding: 10;");
                        recentFileContainer.getChildren().add(emptyLbl);
                    } else {
                        for (ListItem.FileDto f : files) {
                            recentFileContainer.getChildren().add(createFileRow(f));
                        }
                    }
                });
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    private HBox createFileRow(ListItem.FileDto file) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color:#FFFFFF; -fx-border-color:rgba(0,0,0,0.1); -fx-border-width:1; -fx-background-radius:10;");
        row.setPadding(new Insets(15, 15, 15, 15));
        row.setCursor(Cursor.HAND); // ✅ Thêm con trỏ tay

        ImageView icon = new ImageView(IconHelper.getFileIcon("FILE", file.originalFilename));
        icon.setFitWidth(24); icon.setFitHeight(24);

        Label nameLabel = new Label(file.originalFilename);
        nameLabel.setStyle("-fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label sizeLabel = new Label(formatSize(file.size));
        sizeLabel.setPrefWidth(80);

        row.getChildren().addAll(icon, nameLabel, spacer, sizeLabel);

        // ✅ Thêm hiệu ứng Hover cho đẹp
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color:#F0F8FF; -fx-border-color:#2196F3; -fx-border-width:1; -fx-background-radius:10;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-background-color:#FFFFFF; -fx-border-color:rgba(0,0,0,0.1); -fx-border-width:1; -fx-background-radius:10;"));

        // Sự kiện click
        row.setOnMouseClicked(e -> System.out.println("Mở file: " + file.originalFilename));

        return row;
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

        // Vòng tròn nền
        Arc bgArc = new Arc(40, 40, 35, 35, 90, 360);
        bgArc.setType(ArcType.OPEN);
        bgArc.setStroke(Color.rgb(255,255,255,0.3));
        bgArc.setStrokeWidth(8);
        bgArc.setFill(Color.TRANSPARENT);

        // Vòng tròn tiến trình
        Arc progArc = new Arc(40, 40, 35, 35, 90, 0);
        progArc.setType(ArcType.OPEN);
        progArc.setStroke(color);
        progArc.setStrokeWidth(8);
        progArc.setFill(Color.TRANSPARENT);
        progArc.setStrokeLineCap(StrokeLineCap.ROUND);

        Label lbl = new Label((int)percent + "%");
        lbl.setTextFill(color);
        lbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        stackPane.getChildren().addAll(bgArc, progArc, lbl);

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, e -> {
                    progArc.setLength(0);
                    lbl.setText("0%");
                }),
                new KeyFrame(Duration.seconds(1), e -> {
                    progArc.setLength(-(percent * 3.6));
                    lbl.setText((int)percent + "%");
                })
        );
        timeline.play();
    }
}