package com.example.demo.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

public class TrashController {

    @FXML
    private ListView<Object> fileListView;

    public static class Section {
        String title;
        public Section(String title) { this.title = title; }
    }

    public static class FileItem {
        String name;
        Image ownerAvatar;
        String owner;
        String size;
        String location;

        public FileItem(String name, Image ownerAvatar, String owner, String size, String location) {
            this.name = name;
            this.ownerAvatar = ownerAvatar;
            this.owner = owner;
            this.size = size;
            this.location = location;
        }
    }

    @FXML
    private void initialize() {
        ObservableList<Object> items = FXCollections.observableArrayList();

        // ========= DỮ LIỆU MẪU =========
        items.add(new Section("Today"));
        items.add(new FileItem("File của Sơn",
                load("/com/example/demo/imgs/man.png"), "Ahihi", "2MB", "22/12/2025"));
        items.add(new FileItem("File của Sơn",
                load("/com/example/demo/imgs/man.png"), "Ahihi", "2MB", "22/12/2025"));
        items.add(new FileItem("File của Sơn",
                load("/com/example/demo/imgs/man.png"), "Ahihi", "2MB", "22/12/2025"));

        items.add(new Section("Earlier this week"));
        items.add(new FileItem("File của Sơn",
                load("/com/example/demo/imgs/man.png"), "Ahihi", "2MB", "22/12/2025"));
        fileListView.setItems(items);
        fileListView.setCellFactory(list -> new MixedCell());

        // ======= AUTO CHIỀU CAO =======
        fileListView.setFixedCellSize(60);
        fileListView.setPrefHeight(Region.USE_COMPUTED_SIZE);
        fileListView.setMinHeight(Region.USE_PREF_SIZE);
        fileListView.setMaxHeight(Double.MAX_VALUE); // cho phép mở rộng
    }

    private Image load(String path) {
        try {
            return new Image(getClass().getResourceAsStream(path));
        } catch (Exception e) {
            return null;
        }
    }

    private class MixedCell extends ListCell<Object> {
        @Override
        protected void updateItem(Object obj, boolean empty) {
            super.updateItem(obj, empty);
            if (empty || obj == null) {
                setGraphic(null);
                return;
            }
            if (obj instanceof Section) {
                Label section = new Label(((Section) obj).title);
                section.getStyleClass().add("section-header");
                setGraphic(section);
            } else {
                FileItem f = (FileItem) obj;
                setGraphic(buildRow(f));
            }
        }
    }

    private GridPane buildRow(FileItem f) {
        GridPane gp = new GridPane();
        gp.getStyleClass().add("row");
        gp.setHgap(10);
        gp.setAlignment(Pos.CENTER_LEFT);
        gp.setPrefHeight(48);

        ColumnConstraints c0 = new ColumnConstraints(530);
        ColumnConstraints c1 = new ColumnConstraints(140);
        ColumnConstraints c2 = new ColumnConstraints(100);
        ColumnConstraints c3 = new ColumnConstraints(120);
        ColumnConstraints c4 = new ColumnConstraints(40); // thêm cột sao
        gp.getColumnConstraints().addAll(c0, c1, c2, c3, c4);

        // --- Name ---
        HBox nameBox = new HBox(10);
        nameBox.setAlignment(Pos.CENTER_LEFT);
        Label name = new Label(f.name);
        name.getStyleClass().add("file-name");
        nameBox.getChildren().add(name);

        // --- Owner ---
        HBox ownerBox = new HBox(8);
        ownerBox.setAlignment(Pos.CENTER_LEFT);
        if (f.ownerAvatar != null) {
            ImageView av = new ImageView(f.ownerAvatar);
            av.setFitWidth(22);
            av.setFitHeight(22);
            ownerBox.getChildren().add(av);
        }
        Label owner = new Label(f.owner);
        ownerBox.getChildren().add(owner);

        // --- Size ---
        Label size = new Label(f.size);

        // --- Location ---
        Label loc = new Label(f.location);
        loc.getStyleClass().add("badge");
        HBox locBox = new HBox(loc);
        locBox.setAlignment(Pos.CENTER_LEFT);

        // --- Star button ---
        ImageView star = new ImageView(new Image(getClass().getResourceAsStream("/com/example/demo/imgs/bin.png")));
        star.setFitWidth(18);
        star.setFitHeight(18);
        star.getStyleClass().add("star-icon");

        // Khi click thì bỏ sao (hoặc xóa khỏi danh sách)
        star.setOnMouseClicked(e -> {
            fileListView.getItems().remove(f);
            // TODO: thêm logic gọi API hoặc cập nhật backend nếu cần
        });

        gp.add(nameBox, 0, 0);
        gp.add(ownerBox, 1, 0);
        gp.add(size, 2, 0);
        gp.add(locBox, 3, 0);
        gp.add(star, 4, 0); // thêm cột sao ở cuối

        return gp;
    }

}
