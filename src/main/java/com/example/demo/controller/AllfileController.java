package com.example.demo.controller;

import java.awt.Desktop; // Dùng để mở file hệ thống
import java.io.File;
import java.io.IOException;
import java.util.Stack;
import java.util.concurrent.Executors;

import com.example.demo.ClientApiHandler;
import com.example.demo.ClientApiHandler.DirectoryContentResponse;
import com.example.demo.ListItem;
import com.example.demo.util.IconHelper;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

public class AllfileController {

    @FXML private MenuButton btnNew;
    @FXML private MenuItem menuNewFolder;
    @FXML private MenuItem menuFileUpload;
    @FXML private MenuItem menuFolderUpload;

    // Nút Back (cần có trong FXML để quay lại thư mục cha)
    @FXML private Button btnBack;

    // Hai vùng chứa dữ liệu
    @FXML private FlowPane folderFlowPane;
    @FXML private FlowPane fileFlowPane;

    @FXML private ImageView avatarBtn;

    private Long currentDirectoryId = null; // null = Root
    private Stack<Long> historyStack = new Stack<>();

    @FXML
    public void initialize() {
        menuNewFolder.setOnAction(e -> handleCreateFolder());
        menuFileUpload.setOnAction(e -> handleUploadFile());
        menuFolderUpload.setOnAction(e -> handleUploadFolder());

        if (btnBack != null) {
            btnBack.setOnAction(e -> handleBack());
            btnBack.setDisable(true);
        }

        loadFiles();
    }

    // --- TẢI DỮ LIỆU ---
    private void loadFiles() {
        if (folderFlowPane != null) folderFlowPane.getChildren().clear();
        if (fileFlowPane != null) fileFlowPane.getChildren().clear();

        Executors.newSingleThreadExecutor().execute(() -> {
            DirectoryContentResponse content = ClientApiHandler.getDirectoryContent(currentDirectoryId);

            Platform.runLater(() -> {
                if (content == null) return;

                // 1. Hiển thị Folder
                if (content.directories != null && folderFlowPane != null) {
                    for (ListItem.DirectoryDto dir : content.directories) {
                        folderFlowPane.getChildren().add(createFolderItem(dir));
                    }
                }

                // 2. Hiển thị File
                if (content.files != null && fileFlowPane != null) {
                    for (ListItem.FileDto file : content.files) {
                        fileFlowPane.getChildren().add(createFileItem(file));
                    }
                }

                // Update nút Back
                if (btnBack != null) {
                    btnBack.setDisable(currentDirectoryId == null);
                }
            });
        });
    }

    // --- GIAO DIỆN FOLDER (Hỗ trợ mở & xóa) ---
    private VBox createFolderItem(ListItem.DirectoryDto dir) {
        VBox vbox = new VBox(5);
        vbox.setAlignment(Pos.CENTER);
        vbox.setPadding(new Insets(10));
        vbox.setPrefSize(100, 100);

        ImageView icon = new ImageView();
        try {
            icon.setImage(new Image(getClass().getResourceAsStream("/icons/folder.png")));
        } catch (Exception e) {}
        icon.setFitWidth(48); icon.setFitHeight(48);

        Label nameLbl = new Label(dir.name);
        nameLbl.setMaxWidth(90);
        if (dir.name.length() > 12) nameLbl.setText(dir.name.substring(0, 9) + "...");

        vbox.getChildren().addAll(icon, nameLbl);

        // Style & Hover
        vbox.setStyle("-fx-background-radius: 5; -fx-cursor: hand;");
        vbox.setOnMouseEntered(e -> vbox.setStyle("-fx-background-color: #e3f2fd; -fx-background-radius: 5; -fx-cursor: hand;"));
        vbox.setOnMouseExited(e -> vbox.setStyle("-fx-background-color: transparent;"));

        // ✅ 1. SỰ KIỆN MỞ FOLDER (Double Click)
        vbox.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                historyStack.push(currentDirectoryId == null ? -1L : currentDirectoryId);
                currentDirectoryId = dir.id;
                loadFiles();
            }
        });

        // ✅ 2. MENU XÓA FOLDER (Chuột phải)
        ContextMenu cm = new ContextMenu();
        MenuItem deleteItem = new MenuItem("Xóa thư mục");
        deleteItem.setStyle("-fx-text-fill: red;");
        deleteItem.setOnAction(ev -> handleDeleteFolder(dir.id));
        cm.getItems().add(deleteItem);
        vbox.setOnContextMenuRequested(ev -> cm.show(vbox, ev.getScreenX(), ev.getScreenY()));

        return vbox;
    }

    // --- GIAO DIỆN FILE (Hỗ trợ đọc/mở & xóa) ---
    private VBox createFileItem(ListItem.FileDto file) {
        VBox vbox = new VBox(5);
        vbox.setAlignment(Pos.CENTER);
        vbox.setPadding(new Insets(10));
        vbox.setPrefSize(100, 100);

        ImageView icon = new ImageView(IconHelper.getFileIcon("FILE", file.originalFilename));
        icon.setFitWidth(48); icon.setFitHeight(48);

        Label nameLbl = new Label(file.originalFilename);
        nameLbl.setMaxWidth(90);
        if (file.originalFilename.length() > 12) nameLbl.setText(file.originalFilename.substring(0, 9) + "...");

        vbox.getChildren().addAll(icon, nameLbl);

        vbox.setStyle("-fx-background-radius: 5; -fx-cursor: hand;");
        vbox.setOnMouseEntered(e -> vbox.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 5;"));
        vbox.setOnMouseExited(e -> vbox.setStyle("-fx-background-color: transparent;"));

        // ✅ 3. SỰ KIỆN ĐỌC FILE / MỞ FILE (Double Click)
        vbox.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                handleOpenFile(file);
            }
        });

        // ✅ 4. MENU FILE (Tải xuống & Xóa)
        ContextMenu cm = new ContextMenu();
        MenuItem open = new MenuItem("Mở file");
        open.setOnAction(ev -> handleOpenFile(file));

        MenuItem download = new MenuItem("Tải xuống máy");
        download.setOnAction(ev -> handleDownloadFile(file.id, file.originalFilename));

        MenuItem delete = new MenuItem("Xóa file");
        delete.setStyle("-fx-text-fill: red;");
        delete.setOnAction(ev -> handleDeleteFile(file.id));

        cm.getItems().addAll(open, download, new SeparatorMenuItem(), delete);
        vbox.setOnContextMenuRequested(ev -> cm.show(vbox, ev.getScreenX(), ev.getScreenY()));

        return vbox;
    }

    // --- XỬ LÝ LOGIC ---

    // Logic ĐỌC FILE (Tải về temp -> Mở bằng Desktop)
    private void handleOpenFile(ListItem.FileDto fileDto) {
        Executors.newSingleThreadExecutor().execute(() -> {
            // Tải file về thư mục tạm
            File tempFile = ClientApiHandler.downloadFileToTemp(fileDto.id, fileDto.originalFilename);

            Platform.runLater(() -> {
                if (tempFile != null && tempFile.exists()) {
                    try {
                        // Mở file bằng trình mặc định của hệ điều hành
                        Desktop.getDesktop().open(tempFile);
                    } catch (IOException e) {
                        showAlert("Lỗi", "Không thể mở file này: " + e.getMessage(), Alert.AlertType.ERROR);
                    }
                } else {
                    showAlert("Lỗi", "Không thể tải nội dung file.", Alert.AlertType.ERROR);
                }
            });
        });
    }

    private void handleBack() {
        if (!historyStack.isEmpty()) {
            Long prev = historyStack.pop();
            currentDirectoryId = (prev == -1L) ? null : prev;
            loadFiles();
        }
    }

    private void handleDeleteFile(Long fileId) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Bạn chắc chắn muốn xóa file này?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                Executors.newSingleThreadExecutor().execute(() -> {
                    if (ClientApiHandler.deleteFile(fileId)) {
                        Platform.runLater(this::loadFiles);
                    } else {
                        Platform.runLater(() -> showAlert("Lỗi", "Xóa file thất bại", Alert.AlertType.ERROR));
                    }
                });
            }
        });
    }

    private void handleDeleteFolder(Long folderId) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Xóa thư mục này sẽ xóa cả nội dung bên trong?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                Executors.newSingleThreadExecutor().execute(() -> {
                    // Gọi API xóa folder
                    try {
                        java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                                .uri(java.net.URI.create("http://localhost:8080/api/directories/" + folderId))
                                .header("Authorization", "Bearer " + ClientApiHandler.jwtToken)
                                .DELETE().build();
                        java.net.http.HttpClient.newHttpClient().send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
                    } catch (Exception e) {}

                    try { Thread.sleep(500); } catch (Exception e) {}
                    Platform.runLater(this::loadFiles);
                });
            }
        });
    }

    // Các hàm Upload/Tạo Folder giữ nguyên
    private void handleCreateFolder() {
        TextInputDialog dialog = new TextInputDialog("New Folder");
        dialog.setTitle("Tạo thư mục");
        dialog.setHeaderText("Nhập tên thư mục mới:");
        dialog.showAndWait().ifPresent(name -> {
            Executors.newSingleThreadExecutor().execute(() -> {
                ClientApiHandler.createDirectoryAndGetId(name, currentDirectoryId);
                Platform.runLater(this::loadFiles);
            });
        });
    }

    private void handleUploadFile() {
        FileChooser fc = new FileChooser();
        File f = fc.showOpenDialog(btnNew.getScene().getWindow());
        if (f != null) {
            Executors.newSingleThreadExecutor().execute(() -> {
                ClientApiHandler.uploadFile(f, currentDirectoryId);
                Platform.runLater(this::loadFiles);
            });
        }
    }

    private void handleUploadFolder() {
        DirectoryChooser dc = new DirectoryChooser();
        File f = dc.showDialog(btnNew.getScene().getWindow());
        if (f != null) {
            Executors.newSingleThreadExecutor().execute(() -> {
                ClientApiHandler.uploadFolderRecursive(f, currentDirectoryId);
                Platform.runLater(this::loadFiles);
            });
        }
    }

    private void handleDownloadFile(Long id, String name) {
        FileChooser fc = new FileChooser();
        fc.setInitialFileName(name);
        File dest = fc.showSaveDialog(btnNew.getScene().getWindow());
        if (dest != null) {
            Executors.newSingleThreadExecutor().execute(() -> {
                File temp = ClientApiHandler.downloadFileToTemp(id, name);
                if (temp != null) {
                    try {
                        java.nio.file.Files.copy(temp.toPath(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        Platform.runLater(() -> showAlert("Thành công", "Đã tải file về máy", Alert.AlertType.INFORMATION));
                    } catch (IOException e) {}
                }
            });
        }
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.show();
    }
}