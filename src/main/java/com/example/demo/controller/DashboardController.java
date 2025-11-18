package com.example.demo.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;

public class DashboardController {

    // CHỈ CÒN CÁC THÀNH PHẦN THUỘC dashboard_content.fxml
    @FXML
    private StackPane storageCircle;
    @FXML private StackPane transferCircle;

    @FXML
    public void initialize() {

        // Tiến trình phần storage
        createCircularProgress(storageCircle, 77, Color.web("#ffffff"));

        // Tiến trình phần transfer
        createCircularProgress(transferCircle, 10, Color.web("#A78BFA"));
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
