package com.example.demo.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.stage.Popup;
import javafx.util.Duration;

public class DashboardController {

    // CHỈ CÒN CÁC THÀNH PHẦN THUỘC dashboard_content.fxml
    @FXML
    private StackPane storageCircle;
    @FXML private StackPane transferCircle;
    @FXML private javafx.scene.image.ImageView avatarBtn;

    @FXML
    public void initialize() {
        setupAvatarMenu();

        // Tiến trình phần storage
        createCircularProgress(storageCircle, 77, Color.web("#ffffff"));

        // Tiến trình phần transfer
        createCircularProgress(transferCircle, 10, Color.web("#A78BFA"));
    }
    // ==================== AVATAR MENU ====================
    private void setupAvatarMenu() {

        Popup popup = new Popup();
        popup.setAutoHide(true);

        VBox box = new VBox();
        box.setSpacing(5);
        box.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
        box.setPadding(new Insets(10));
        box.setEffect(new javafx.scene.effect.DropShadow(10, Color.rgb(0, 0, 0, 0.18)));

        // Sử dụng lại createMenuRow() giống menu "+ New"
        VBox profile = createMenuRow("Thông tin cá nhân");
        VBox account = createMenuRow("Tài khoản");
        VBox logout = createMenuRow("Đăng xuất");

        box.getChildren().addAll(profile, account, logout);
        popup.getContent().add(box);

        // Hiện popup khi nhấn vào avatar
        avatarBtn.setOnMouseClicked(e -> {
            popup.show(
                    avatarBtn,
                    avatarBtn.localToScreen(avatarBtn.getBoundsInLocal()).getMaxX() - 120,
                    avatarBtn.localToScreen(avatarBtn.getBoundsInLocal()).getMaxY() + 5
            );
        });

        // Sự kiện bấm từng item
        profile.setOnMouseClicked(e -> {
            popup.hide();
            System.out.println("Đi đến trang thông tin cá nhân");
        });

        account.setOnMouseClicked(e -> {
            popup.hide();
            System.out.println("Trang tài khoản");
        });

        logout.setOnMouseClicked(e -> {
            popup.hide();
            System.out.println("Đăng xuất…");

            // Nếu muốn trở lại login → đưa Main vào AllfileController
            // mainApp.showLoginScene();
        });
    }
    /** Tạo dòng menu với hiệu ứng hover */
    private VBox createMenuRow(String title) {
        HBox row = new HBox(10);
        row.setPadding(new Insets(10));
        row.setCursor(Cursor.HAND);

        Label label = new Label(title);
        label.setStyle("-fx-font-size: 14px;");

        row.getChildren().add(label);

        row.setOnMouseEntered(e ->
                row.setStyle("-fx-background-color: #EEF6FF; -fx-background-radius: 6;")
        );
        row.setOnMouseExited(e ->
                row.setStyle("-fx-background-color: transparent;")
        );

        VBox wrapper = new VBox(row);
        return wrapper;
    }


    /* ==========================================================================================
     *                                  VÒNG TRÒN TIẾN TRÌNH
     * ========================================================================================== */
    private void createCircularProgress(StackPane stackPane, double percent, Color color) {
        stackPane.getChildren().clear();
        stackPane.setPrefSize(80, 80);

        Arc backgroundArc = new Arc(40, 40, 35, 35, 90, 360);
        backgroundArc.setType(ArcType.OPEN);
        backgroundArc.setStroke(Color.web("#EEEEEE"));
        backgroundArc.setStrokeWidth(8);
        backgroundArc.setFill(Color.TRANSPARENT);
        backgroundArc.setStrokeLineCap(StrokeLineCap.ROUND);

        Arc progressArc = new Arc(40, 40, 35, 35, 90, 0);
        progressArc.setType(ArcType.OPEN);
        progressArc.setStroke(color);
        progressArc.setStrokeWidth(8);
        progressArc.setFill(Color.TRANSPARENT);
        progressArc.setStrokeLineCap(StrokeLineCap.ROUND);

        Label percentLabel = new Label("0%");
        percentLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        stackPane.getChildren().addAll(backgroundArc, progressArc, percentLabel);

        Timeline timeline = new Timeline();
        int steps = (int) percent;

        for (int i = 1; i <= steps; i++) {
            int value = i;
            KeyFrame kf = new KeyFrame(Duration.millis(i * 10), e -> {
                progressArc.setLength(-360 * value / 100.0);
                percentLabel.setText(value + "%");
            });
            timeline.getKeyFrames().add(kf);
        }
        timeline.play();
    }
}
