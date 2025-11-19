package com.example.demo.controller;

import java.io.File;
import java.util.List;
import java.util.concurrent.Executors;

import com.example.demo.ClientApiHandler;
import com.example.demo.ListItem;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Popup;

public class AllfileController {

    @FXML private Button btnNew;

    @FXML private FlowPane folderFlowPane;
    @FXML private FlowPane fileFlowPane;

    @FXML private javafx.scene.image.ImageView avatarBtn;



    // ✅ SỬA 1: Bỏ 'final' để có thể cập nhật ID thư mục
    private Long currentDirectoryId = null; // null = root

    @FXML
    public void initialize() {

        System.out.println("Allfile content loaded!");
        setupNewMenu();

        loadDataFromServer();
    }

    // ================= TẢI DỮ LIỆU =================
    private void loadDataFromServer() {
        if (!ClientApiHandler.isAuthenticated()) {
            showEmptyMessage("Vui lòng đăng nhập để xem dữ liệu");
            return;
        }

        folderFlowPane.getChildren().clear();
        fileFlowPane.getChildren().clear();
        folderFlowPane.getChildren().add(createLoadingCard());
        fileFlowPane.getChildren().add(createLoadingCard());

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Tải dữ liệu dựa trên currentDirectoryId hiện tại
                List<ListItem> folders = fetchDirectories(currentDirectoryId);
                List<ListItem> files = fetchFiles(currentDirectoryId);

                Platform.runLater(() -> {
                    folderFlowPane.getChildren().clear();
                    fileFlowPane.getChildren().clear();

                    // Thêm nút quay lại nếu không phải là thư mục gốc
                    if (currentDirectoryId != null) {
                        folderFlowPane.getChildren().add(createBackCard());
                    }

                    renderFolders(folders);
                    renderFiles(files);

                    if (folders.isEmpty() && currentDirectoryId == null) {
                        folderFlowPane.getChildren().add(createEmptyCard("Không có thư mục"));
                    } else if (folders.isEmpty() && currentDirectoryId != null && folderFlowPane.getChildren().size() == 1) {
                         // Nếu chỉ có nút Back và không có folder nào
                         folderFlowPane.getChildren().add(createEmptyCard("Thư mục trống"));
                    }

                    if (files.isEmpty()) fileFlowPane.getChildren().add(createEmptyCard("Không có file nào"));
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    folderFlowPane.getChildren().clear();
                    fileFlowPane.getChildren().clear();
                    folderFlowPane.getChildren().add(createEmptyCard("Lỗi tải dữ liệu: " + e.getMessage()));
                    fileFlowPane.getChildren().add(createEmptyCard(""));
                });
                e.printStackTrace();
            }
        });
    }
    
    // ✅ SỬA 2: Thêm phương thức để cập nhật ID thư mục và tải lại
    private void openDirectory(Long directoryId) {
        if (directoryId != null) {
            System.out.println("--- Đang chuyển thư mục đến ID: " + directoryId + " ---");
        } else {
            System.out.println("--- Đang trở về thư mục Gốc ---");
        }
        this.currentDirectoryId = directoryId;
        loadDataFromServer();
    }

    // ================= RENDER THƯ MỤC =================
    private void renderFolders(List<ListItem> folders) {
        for (ListItem item : folders) {
            folderFlowPane.getChildren().add(createFolderCard(item));
        }
    }
    
    // Thẻ quay lại (Tạm thời quay về gốc vì thiếu thông tin parentId từ Server)
    private VBox createBackCard() {
        VBox card = new VBox(12);
        card.setPrefWidth(230);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: #F8F8F8; -fx-background-radius: 12; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0.2, 0, 3);");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setCursor(Cursor.HAND);

        // Icon (có thể thay bằng icon local)
        ImageView icon = new ImageView("https://img.icons8.com/ios-filled/50/reply-arrow.png");
        icon.setFitWidth(32);
        icon.setFitHeight(32);

        Label nameLabel = new Label("..");
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label typeLabel = new Label("Quay lại");
        typeLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");

        card.getChildren().addAll(icon, nameLabel, typeLabel);
        
        card.setOnMouseClicked(e -> {
            // Quay về thư mục gốc (hoặc Parent Directory ID nếu có)
            openDirectory(null); 
        });

        return card;
    }


    private VBox createFolderCard(ListItem item) {
        VBox card = new VBox(12);
        card.setPrefWidth(230);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0.2, 0, 3);");
        card.setCursor(Cursor.HAND);

        // Icon thư mục (dùng online)
        ImageView icon = new ImageView("https://img.icons8.com/fluency/48/folder-invoices.png");
        icon.setFitWidth(32);
        icon.setFitHeight(32);

        Label nameLabel = new Label(item.getName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        nameLabel.setMaxWidth(200);
        nameLabel.setWrapText(true);

        Label typeLabel = new Label("Thư mục");
        typeLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");

        HBox top = new HBox(10, icon, new Region());
        HBox.setHgrow(top.getChildren().get(1), Priority.ALWAYS);
        MenuButton menu = new MenuButton("...");
        menu.setStyle("-fx-background-color: transparent; -fx-font-size: 18px;");
        top.getChildren().add(menu);

        card.getChildren().addAll(top, nameLabel, typeLabel);

        // ✅ SỬA 3: Double click để vào thư mục
        card.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                System.out.println("Mở thư mục: " + item.getName() + " (ID: " + item.getId() + ")");
                openDirectory(item.getId()); // Gọi hàm chuyển thư mục
            }
        });

        return card;
    }

    // ================= RENDER FILE (Không đổi) =================
    private void renderFiles(List<ListItem> files) {
        for (ListItem item : files) {
            fileFlowPane.getChildren().add(createFileCard(item));
        }
    }

    private VBox createFileCard(ListItem item) {
        VBox card = new VBox(10);
        card.setPrefWidth(230);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0.2, 0, 3);");

        StackPane preview = new StackPane();
        preview.setPrefSize(206, 130);
        preview.setStyle("-fx-background-color: #F5F5F5; -fx-background-radius: 10;");

        String type = item.getType() != null ? item.getType().toLowerCase() : "";
        String iconUrl = "https://img.icons8.com/color/96/file.png";
        if (type.contains("pdf")) iconUrl = "https://img.icons8.com/color/96/pdf.png";
        else if (type.contains("image") || type.contains("png") || type.contains("jpg"))
            iconUrl = "https://img.icons8.com/color/96/image.png";
        else if (type.contains("video") || type.contains("mp4"))
            iconUrl = "https://img.icons8.com/color/96/video.png";

        ImageView previewIcon = new ImageView(iconUrl);
        previewIcon.setFitWidth(60);
        previewIcon.setFitHeight(60);
        preview.getChildren().add(previewIcon);

        HBox bottom = new HBox(8);
        bottom.setAlignment(Pos.CENTER_LEFT);
        CheckBox check = new CheckBox();
        Label name = new Label(item.getName());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        name.setMaxWidth(140);
        name.setWrapText(true);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        MenuButton menu = new MenuButton("...");
        menu.setStyle("-fx-background-color: transparent;");

        bottom.getChildren().addAll(check, name, spacer, menu);

        Label size = new Label(item.getSize());
        size.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");

        card.getChildren().addAll(preview, bottom, size);
        return card;
    }

    // ================= CÁC PHƯƠNG THỨC KHÁC (Không đổi) =================
    private VBox createLoadingCard() {
        return createEmptyCard("Đang tải...");
    }

    private VBox createEmptyCard(String msg) {
        VBox v = new VBox(10);
        v.setAlignment(Pos.CENTER);
        v.setPrefWidth(230);
        v.setPadding(new Insets(40));
        Label l = new Label(msg);
        l.setStyle("-fx-text-fill: #999; -fx-font-size: 14px;");
        v.getChildren().add(l);
        return v;
    }

    private void showEmptyMessage(String msg) {
        Platform.runLater(() -> {
            folderFlowPane.getChildren().add(createEmptyCard(msg));
            fileFlowPane.getChildren().add(createEmptyCard(""));
        });

        setupAvatarMenu();

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


    private void setupNewMenu() {
        Popup popup = new Popup();
        popup.setAutoHide(true);
        VBox box = new VBox(5);
        box.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, black, 10, 0.2, 0, 3);");
        box.setPadding(new Insets(10));

        VBox newFolder = createMenuRow("New Folder");
        VBox uploadFile = createMenuRow("Upload File");
        VBox uploadFolder = createMenuRow("Upload Folder");

        newFolder.setOnMouseClicked(e -> { popup.hide(); showNewFolderDialog(); });
        uploadFile.setOnMouseClicked(e -> { popup.hide(); handleUploadFile(); });
        uploadFolder.setOnMouseClicked(e -> { popup.hide(); handleUploadFolder(); });

        box.getChildren().addAll(newFolder, uploadFile, uploadFolder);
        popup.getContent().add(box);

        btnNew.setOnAction(e -> popup.show(btnNew,
                btnNew.localToScreen(btnNew.getBoundsInLocal()).getMinX(),
                btnNew.localToScreen(btnNew.getBoundsInLocal()).getMaxY() + 5));
    }

    private VBox createMenuRow(String title) {
        HBox row = new HBox(10);
        row.setPadding(new Insets(10));
        row.setCursor(Cursor.HAND);
        Label label = new Label(title);
        label.setStyle("-fx-font-size: 14px;");
        row.getChildren().add(label);
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: #EEF6FF; -fx-background-radius: 6;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-background-color: transparent;"));
        return new VBox(row);
    }

    private void showNewFolderDialog() {
        if (!ClientApiHandler.isAuthenticated()) return;
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Tạo thư mục mới");
        dialog.setHeaderText("Nhập tên thư mục");
        dialog.showAndWait().ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                ClientApiHandler.createFolder(name.trim(), currentDirectoryId);
                loadDataFromServer();
            }
        });
    }

    private void handleUploadFile() {
        if (!ClientApiHandler.isAuthenticated()) return;
        FileChooser chooser = new FileChooser();
        File file = chooser.showOpenDialog(btnNew.getScene().getWindow());
        if (file != null) {
            ClientApiHandler.uploadFile(file, currentDirectoryId);
            loadDataFromServer();
        }
    }

    private void handleUploadFolder() {
        if (!ClientApiHandler.isAuthenticated()) return;
        DirectoryChooser chooser = new DirectoryChooser();
        File dir = chooser.showDialog(btnNew.getScene().getWindow());
        if (dir != null) {
            ClientApiHandler.uploadDirectory(dir, currentDirectoryId);
            loadDataFromServer();
        }
    }

    // ================= LẤY DỮ LIỆU TỪ SERVER =================
    private List<ListItem> fetchDirectories(Long parentId) throws Exception {
        List<ListItem.DirectoryDto> dtos = ClientApiHandler.getDirectories(parentId);
        return dtos.stream().map(ListItem::new).toList();
    }

    private List<ListItem> fetchFiles(Long directoryId) throws Exception {
        List<ListItem.FileDto> dtos = ClientApiHandler.getFiles(directoryId);
        return dtos.stream().map(ListItem::new).toList();
    }
}