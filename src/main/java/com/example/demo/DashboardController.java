package com.example.demo;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.Blend;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.ColorInput;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;

import java.util.Arrays;
import java.util.List;

public class DashboardController {

    // Sidebar icon
    @FXML private ImageView iconDashboard;
    @FXML private ImageView iconFile;
    @FXML private ImageView iconShared;
    @FXML private ImageView iconInbox;
    @FXML private ImageView iconRecent;
    @FXML private ImageView iconStar;
    @FXML private ImageView iconTrash;

    // Sidebar button
    @FXML private Button btnDashboard;
    @FXML private Button btnAllFile;
    @FXML private Button btnShared;
    @FXML private Button btnInbox;
    @FXML private Button btnRecent;
    @FXML private Button btnStarred;
    @FXML private Button btnTrash;

    private List<Button> sidebarButtons;

    // Dashboard progress circles
    @FXML private StackPane storageCircle;
    @FXML private StackPane transferCircle;

    @FXML
    public void initialize() {

        /* ====== TÔ MÀU ICON SIDEBAR ====== */
        Color gray = Color.web("#9e9e9e");
        tintIcon(iconDashboard, gray);
        tintIcon(iconFile, gray);
        tintIcon(iconShared, gray);
        tintIcon(iconInbox, gray);
        tintIcon(iconRecent, gray);
        tintIcon(iconStar, gray);
        tintIcon(iconTrash, gray);


        /* ====== SETUP SIDEBAR BUTTON ====== */
        sidebarButtons = Arrays.asList(
                btnDashboard, btnAllFile, btnShared,
                btnInbox, btnRecent, btnStarred, btnTrash
        );

        // Gán sự kiện click để highlight nút
        sidebarButtons.forEach(btn ->
                btn.setOnAction(e -> setActiveSidebarButton(btn))
        );

        // Mặc định chọn Dashboard
        setActiveSidebarButton(btnDashboard);


        /* ====== TIẾN TRÌNH STORAGE / TRANSFER ====== */
        createCircularProgress(storageCircle, 77, Color.web("#ffffff"));
        createCircularProgress(transferCircle, 10, Color.web("#A78BFA"));
    }


    /* ==========================================================================================
     *                                  SIDEBAR ACTIVE BUTTON
     * ========================================================================================== */
    private void setActiveSidebarButton(Button activeBtn) {

        // Xóa active ở tất cả button
        sidebarButtons.forEach(btn -> {
            btn.getStyleClass().remove("sidebar-button-active");
        });

        // Thêm active vào button vừa click
        if (!activeBtn.getStyleClass().contains("sidebar-button-active")) {
            activeBtn.getStyleClass().add("sidebar-button-active");
        }
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


    /* ==========================================================================================
     *                                  TÔ MÀU ICON SIDEBAR
     * ========================================================================================== */
    private void tintIcon(ImageView imageView, Color color) {
        ColorAdjust adjust = new ColorAdjust();
        adjust.setBrightness(-1);

        Blend blend = new Blend(
                BlendMode.SRC_ATOP,
                adjust,
                new ColorInput(
                        0, 0,
                        imageView.getFitWidth(),
                        imageView.getFitHeight(),
                        color
                )
        );

        imageView.setEffect(blend);
    }
}
