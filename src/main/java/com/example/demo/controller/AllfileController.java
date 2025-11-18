package com.example.demo.controller;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Popup;

public class AllfileController {

    @FXML private Button btnNew;

    @FXML
    public void initialize() {
        System.out.println("Allfile content loaded!");

        setupNewMenu();
    }

    private void setupNewMenu() {

        // Popup custom
        Popup popup = new Popup();
        popup.setAutoHide(true);

        VBox box = new VBox();
        box.setSpacing(5);
        box.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
        box.setPadding(new Insets(10));
        box.setEffect(new javafx.scene.effect.DropShadow(10, Color.rgb(0, 0, 0, 0.18)));

        // == FUNCTION: tạo dòng menu đẹp ==
        VBox newFolder = createMenuRow("New Folder");
        VBox uploadFile = createMenuRow("Upload File");
        VBox uploadFolder = createMenuRow("Upload Folder");

        // Add vào popup
        box.getChildren().addAll(newFolder, uploadFile, uploadFolder);
        popup.getContent().add(box);

        // Show khi nhấn nút
        btnNew.setOnAction(e -> {
            popup.show(btnNew,
                    btnNew.localToScreen(btnNew.getBoundsInLocal()).getMinX(),
                    btnNew.localToScreen(btnNew.getBoundsInLocal()).getMaxY() + 5
            );
        });

        // Action
        newFolder.setOnMouseClicked(e -> {
            popup.hide();
            System.out.println("Action: Create New Folder");
        });

        uploadFile.setOnMouseClicked(e -> {
            popup.hide();
            System.out.println("Action: Upload File");
        });

        uploadFolder.setOnMouseClicked(e -> {
            popup.hide();
            System.out.println("Action: Upload Folder");
        });
    }


    // ==========================
    //  CUSTOM MENU ROW (NO ICON)
    // ==========================
    private VBox createMenuRow(String title) {

        HBox row = new HBox(10);
        row.setPadding(new Insets(10));
        row.setCursor(Cursor.HAND);

        // TEXT
        Label label = new Label(title);
        label.setStyle("-fx-font-size: 14px;");

        row.getChildren().add(label);

        // HOVER EFFECT
        row.setOnMouseEntered(e ->
                row.setStyle("-fx-background-color: #EEF6FF; -fx-background-radius: 6;")
        );
        row.setOnMouseExited(e ->
                row.setStyle("-fx-background-color: transparent;")
        );

        VBox wrapper = new VBox(row);
        return wrapper;
    }
}
