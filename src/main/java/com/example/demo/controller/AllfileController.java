package com.example.demo.controller;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.Executors;

import com.example.demo.ClientApiHandler;
import com.example.demo.ListItem;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class AllfileController {

    @FXML private MenuButton btnNew;
    @FXML private MenuItem menuNewFolder;
    @FXML private MenuItem menuFileUpload;
    @FXML private MenuItem menuFolderUpload;

    @FXML private FlowPane folderFlowPane;
    @FXML private FlowPane fileFlowPane;

    // Biến lưu ID thư mục đang đứng (null là thư mục gốc)
    private Long currentDirectoryId = null;

    @FXML
    public void initialize() {
        // Cấu hình sự kiện cho menu
        menuNewFolder.setOnAction(e -> handleCreateFolder());
        menuFileUpload.setOnAction(e -> handleUploadFile());
        menuFolderUpload.setOnAction(e -> handleUploadFolder());

        // Load dữ liệu lần đầu (Root)
        loadDataFromServer();
    }

    // --- HÀM ĐIỀU HƯỚNG (NAVIGATE) ---
    private void navigateTo(Long folderId) {
        this.currentDirectoryId = folderId;
        System.out.println("📂 Chuyển đến thư mục ID: " + (folderId == null ? "ROOT" : folderId));
        
        // Clear giao diện cũ
        folderFlowPane.getChildren().clear();
        fileFlowPane.getChildren().clear();
        
        loadDataFromServer();
    }

    // --- TẢI DỮ LIỆU ---
    public void loadDataFromServer() {
        if (!ClientApiHandler.isAuthenticated()) {
            showAlert("Chưa đăng nhập", "Vui lòng đăng nhập lại!", Alert.AlertType.WARNING);
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Lấy danh sách Folder & File từ Server
                List<ListItem.DirectoryDto> dirs = ClientApiHandler.getDirectories(currentDirectoryId);
                List<ListItem.FileDto> files = ClientApiHandler.getFiles(currentDirectoryId);

                // Cập nhật UI
                Platform.runLater(() -> updateUI(dirs, files));
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert("Lỗi kết nối", e.getMessage(), Alert.AlertType.ERROR));
            }
        });
    }

    // --- CẬP NHẬT GIAO DIỆN ---
    private void updateUI(List<ListItem.DirectoryDto> directories, List<ListItem.FileDto> files) {
        folderFlowPane.getChildren().clear();
        fileFlowPane.getChildren().clear();

        // 1. Nút "BACK" nếu đang ở thư mục con
        if (currentDirectoryId != null) {
            VBox backCard = createBackCard();
            folderFlowPane.getChildren().add(backCard);
        }

        // 2. Render Folder
        for (ListItem.DirectoryDto dir : directories) {
            VBox card = createCard(dir.name, "/com/example/demo/imgs/folder.png", true, dir.id);
            folderFlowPane.getChildren().add(card);
        }

        // 3. Render File
        for (ListItem.FileDto file : files) {
            String iconPath = "/com/example/demo/imgs/file.png";
            String name = file.originalFilename.toLowerCase();
            if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")) iconPath = "/com/example/demo/imgs/image.png";
            else if (name.endsWith(".pdf")) iconPath = "/com/example/demo/imgs/pdf.png";
            
            VBox card = createCard(file.originalFilename, iconPath, false, file.id);
            fileFlowPane.getChildren().add(card);
        }
    }

    // --- TẠO CARD (HỖ TRỢ CLICK ĐÚP) ---
    private VBox createCard(String title, String iconPath, boolean isFolder, Long itemId) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPrefSize(120, 120);
        
        String defaultStyle = "-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2); -fx-cursor: hand;";
        card.setStyle(defaultStyle);
        
        card.setOnMouseEntered(e -> card.setStyle(defaultStyle + "-fx-background-color: #E3F2FD;"));
        card.setOnMouseExited(e -> card.setStyle(defaultStyle));

        try {
            // Load icon an toàn
            Image img;
            try {
                img = new Image(getClass().getResourceAsStream(iconPath));
            } catch (Exception ex) {
                // Fallback nếu thiếu icon cụ thể
                img = new Image(getClass().getResourceAsStream(isFolder ? "/com/example/demo/imgs/folder.png" : "/com/example/demo/imgs/file.png"));
            }
            ImageView icon = new ImageView(img);
            icon.setFitWidth(40);
            icon.setFitHeight(40);
            card.getChildren().add(icon);
        } catch (Exception e) { /* Bỏ qua */ }

        Label name = new Label(title);
        name.setWrapText(true);
        name.setMaxWidth(100);
        name.setAlignment(Pos.CENTER);
        card.getChildren().add(name);

        // ✅ SỰ KIỆN CLICK ĐÚP
        card.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                if (isFolder) {
                    navigateTo(itemId);
                } else {
                    handleOpenFile(itemId, title);
                }
            }
        });

        return card;
    }

    // --- TẠO CARD BACK ---
    private VBox createBackCard() {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPrefSize(120, 120);
        card.setStyle("-fx-background-color: #f0f0f0; -fx-background-radius: 10; -fx-cursor: hand; -fx-border-color: #ccc; -fx-border-style: dashed; -fx-border-radius: 10;");

        Label lb = new Label("⬅ Quay lại");
        lb.setStyle("-fx-font-weight: bold; -fx-text-fill: #555;");
        card.getChildren().add(lb);

        card.setOnMouseClicked(e -> navigateTo(null)); // Về Root
        return card;
    }

    // ================= XỬ LÝ MỞ FILE (PREVIEW) =================

    private void handleOpenFile(Long fileId, String fileName) {
        Executors.newSingleThreadExecutor().execute(() -> {
            Platform.runLater(() -> showAlert("Đang tải", "Đang tải file để xem...", Alert.AlertType.INFORMATION));

            // 1. Tải file về Temp
            File file = ClientApiHandler.downloadFileToTemp(fileId, fileName);

            Platform.runLater(() -> {
                if (file != null && file.exists()) {
                    String lowerName = fileName.toLowerCase();

                    // 2. Phân loại để hiển thị
                    if (lowerName.endsWith(".png") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".gif") || lowerName.endsWith(".bmp")) {
                        showImagePreview(file, fileName);
                    } 
                    else if (lowerName.endsWith(".txt") || lowerName.endsWith(".java") || lowerName.endsWith(".xml") || lowerName.endsWith(".json") || lowerName.endsWith(".html") || lowerName.endsWith(".css") || lowerName.endsWith(".js")) {
                        showTextPreview(file, fileName);
                    } 
                    else {
                        // File khác -> Mở bằng app ngoài
                        openInExternalApp(file);
                    }
                } else {
                    showAlert("Lỗi", "Không thể tải file về.", Alert.AlertType.ERROR);
                }
            });
        });
    }

    // --- TRÌNH XEM ẢNH ---
    private void showImagePreview(File file, String title) {
        try {
            Stage previewStage = new Stage();
            previewStage.setTitle("Xem ảnh: " + title);

            ImageView imageView = new ImageView(new Image(file.toURI().toString()));
            imageView.setPreserveRatio(true);
            imageView.setFitWidth(800);
            imageView.setFitHeight(600);

            StackPane root = new StackPane(imageView);
            root.setStyle("-fx-background-color: rgba(0,0,0,0.9);"); // Nền tối
            
            // Click để đóng
            root.setOnMouseClicked(e -> previewStage.close());

            Scene scene = new Scene(root, 900, 700);
            previewStage.setScene(scene);
            previewStage.centerOnScreen();
            previewStage.show();
            
        } catch (Exception e) {
            showAlert("Lỗi", "Không thể hiển thị ảnh này.", Alert.AlertType.ERROR);
        }
    }

    // --- TRÌNH ĐỌC TEXT ---
    private void showTextPreview(File file, String title) {
        try {
            Stage previewStage = new Stage();
            previewStage.setTitle("Đọc file: " + title);

            TextArea textArea = new TextArea();
            textArea.setEditable(false);
            textArea.setWrapText(true);
            
            // Đọc nội dung file
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            textArea.setText(content);
            textArea.setStyle("-fx-font-family: 'Consolas', 'Monospaced'; -fx-font-size: 14px;");

            StackPane root = new StackPane(textArea);
            Scene scene = new Scene(root, 800, 600);
            
            previewStage.setScene(scene);
            previewStage.show();

        } catch (Exception e) {
            showAlert("Lỗi", "Không thể đọc nội dung file text.", Alert.AlertType.ERROR);
        }
    }

    // --- MỞ APP NGOÀI ---
    private void openInExternalApp(File file) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            } else {
                showAlert("Thông báo", "Đã tải file về máy (Không hỗ trợ xem trước).", Alert.AlertType.INFORMATION);
            }
        } catch (IOException e) {
            showAlert("Lỗi", "Không tìm thấy ứng dụng để mở file này.", Alert.AlertType.ERROR);
        }
    }

    // ================= CÁC HÀM TẠO/UPLOAD =================

    private void handleCreateFolder() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Folder");
        dialog.setHeaderText("Tên thư mục mới:");
        dialog.showAndWait().ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                ClientApiHandler.createFolder(name.trim(), currentDirectoryId);
                try { Thread.sleep(200); } catch (Exception e) {}
                loadDataFromServer();
            }
        });
    }

    private void handleUploadFile() {
        FileChooser chooser = new FileChooser();
        File file = chooser.showOpenDialog(btnNew.getScene().getWindow());
        if (file != null) {
            ClientApiHandler.uploadFile(file, currentDirectoryId);
            new java.util.Timer().schedule(new java.util.TimerTask() {
                @Override public void run() { Platform.runLater(() -> loadDataFromServer()); }
            }, 1000);
        }
    }

    private void handleUploadFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        File dir = chooser.showDialog(btnNew.getScene().getWindow());
        if (dir != null) {
            ClientApiHandler.uploadDirectory(dir, currentDirectoryId);
            showAlert("Đang tải lên", "Thư mục đang được tải ngầm...", Alert.AlertType.INFORMATION);
        }
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.show();
    }
}