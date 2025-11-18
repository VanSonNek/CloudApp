package com.example.demo;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
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

public class DashboardController {

    @FXML private ImageView iconDashboard;
    @FXML private ImageView iconFile;
    @FXML private ImageView iconShared;
    @FXML private ImageView iconInbox;
    @FXML private ImageView iconRecent;
    @FXML private ImageView iconStar;
    @FXML private ImageView iconTrash;

    @FXML private StackPane storageCircle;
    @FXML private StackPane transferCircle;

    @FXML
    public void initialize() {
        // Tô màu icon sidebar
        Color gray = Color.web("#9e9e9e");
        tintIcon(iconDashboard, gray);
        tintIcon(iconFile, gray);
        tintIcon(iconShared, gray);
        tintIcon(iconInbox, gray);
        tintIcon(iconRecent, gray);
        tintIcon(iconStar, gray);
        tintIcon(iconTrash, gray);

        // Storage 77%
        createCircularProgress(storageCircle, 77, Color.web("#ffffff"));

        // Transfer 10%
        createCircularProgress(transferCircle, 10, Color.web("#A78BFA"));
    }

    /**
     * Tạo vòng tròn tiến độ động
     * @param stackPane StackPane chứa vòng tròn
     * @param percent % tiến độ (0-100)
     * @param color màu vòng tròn
     */
    private void createCircularProgress(StackPane stackPane, double percent, Color color) {
        stackPane.getChildren().clear();
        stackPane.setPrefSize(80, 80);

        // Vòng nền
        Arc backgroundArc = new Arc(40, 40, 35, 35, 90, 360);
        backgroundArc.setType(ArcType.OPEN);
        backgroundArc.setStroke(Color.web("#EEEEEE"));
        backgroundArc.setStrokeWidth(8);
        backgroundArc.setFill(Color.TRANSPARENT);
        backgroundArc.setStrokeLineCap(StrokeLineCap.ROUND);

        // Vòng tiến độ
        Arc progressArc = new Arc(40, 40, 35, 35, 90, 0);
        progressArc.setType(ArcType.OPEN);
        progressArc.setStroke(color); // giữ màu gốc
        progressArc.setStrokeWidth(8);
        progressArc.setFill(Color.TRANSPARENT);
        progressArc.setStrokeLineCap(StrokeLineCap.ROUND);

        // Label %
        Label percentLabel = new Label("0%");
        percentLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        stackPane.getChildren().addAll(backgroundArc, progressArc, percentLabel);

        // Animation từ 0 → percent
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

    /**
     * Tô màu icon bằng Blend + ColorAdjust
     */
    private void tintIcon(ImageView imageView, Color color) {
        ColorAdjust adjust = new ColorAdjust();
        adjust.setBrightness(-1);

        Blend blend = new Blend(
                BlendMode.SRC_ATOP,
                adjust,
                new ColorInput(0, 0,
                        imageView.getFitWidth(),
                        imageView.getFitHeight(),
                        color)
        );

        imageView.setEffect(blend);
    }
}
