package com.example.demo.controller;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.Stack;
import java.util.concurrent.Executors;

import com.example.demo.ClientApiHandler;
import com.example.demo.ClientApiHandler.DirectoryContentResponse;
import com.example.demo.ListItem;
import com.example.demo.util.IconHelper;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Duration;

public class AllfileController {

    @FXML private MenuButton btnNew;
    @FXML private MenuItem menuNewFolder;
    @FXML private MenuItem menuFileUpload;
    @FXML private MenuItem menuFolderUpload;

    @FXML private Button btnBack;

    @FXML private FlowPane folderFlowPane;
    @FXML private FlowPane fileFlowPane;

    @FXML private TextField searchField;

    private Long currentDirectoryId = null; // null = Root
    private Stack<Long> historyStack = new Stack<>();

    // Biến cho tìm kiếm realtime
    private PauseTransition searchDebounce;
    private boolean isSearching = false;

    @FXML
    public void initialize() {
        // 1. Setup Menu Action
        menuNewFolder.setOnAction(e -> handleCreateFolder());
        menuFileUpload.setOnAction(e -> handleUploadFile());
        menuFolderUpload.setOnAction(e -> handleUploadFolder());

        if (btnBack != null) {
            btnBack.setOnAction(e -> handleBack());
            btnBack.setDisable(true);
        }

        // 2. Load dữ liệu ban đầu
        loadFiles();

        // 3. Cấu hình Tìm kiếm tức thì
        setupRealTimeSearch();
    }

    // --- LOGIC TÌM KIẾM TỨC THÌ (REAL-TIME) ---
    private void setupRealTimeSearch() {
        searchDebounce = new PauseTransition(Duration.millis(500)); // Chờ 0.5s sau khi ngừng gõ

        searchDebounce.setOnFinished(event -> {
            String keyword = searchField.getText().trim();
            if (keyword.isEmpty()) {
                isSearching = false;
                loadFiles(); // Reset về view mặc định
            } else {
                performSearch(keyword);
            }
        });

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                searchDebounce.playFromStart(); // Reset timer khi gõ
            });
        }
    }

    private void performSearch(String keyword) {
        isSearching = true;

        if (folderFlowPane != null) folderFlowPane.getChildren().clear();
        if (fileFlowPane != null) fileFlowPane.getChildren().clear();

        Executors.newSingleThreadExecutor().execute(() -> {
            DirectoryContentResponse result = ClientApiHandler.searchFiles(keyword);

            Platform.runLater(() -> {
                if (result == null) return;

                if (result.directories != null && folderFlowPane != null) {
                    for (ListItem.DirectoryDto dir : result.directories) {
                        folderFlowPane.getChildren().add(createFolderItem(dir));
                    }
                }

                if (result.files != null && fileFlowPane != null) {
                    for (ListItem.FileDto file : result.files) {
                        fileFlowPane.getChildren().add(createFileItem(file));
                    }
                }

                if (btnBack != null) btnBack.setDisable(true);
            });
        });
    }

    // --- TẢI DỮ LIỆU FILE & FOLDER ---
    private void loadFiles() {
        if (searchField != null && searchField.getText().isEmpty()) {
            isSearching = false;
        }

        if (folderFlowPane != null) folderFlowPane.getChildren().clear();
        if (fileFlowPane != null) fileFlowPane.getChildren().clear();

        Executors.newSingleThreadExecutor().execute(() -> {
            DirectoryContentResponse content = ClientApiHandler.getDirectoryContent(currentDirectoryId);

            Platform.runLater(() -> {
                if (content == null) return;

                if (content.directories != null && folderFlowPane != null) {
                    for (ListItem.DirectoryDto dir : content.directories) {
                        folderFlowPane.getChildren().add(createFolderItem(dir));
                    }
                }

                if (content.files != null && fileFlowPane != null) {
                    for (ListItem.FileDto file : content.files) {
                        fileFlowPane.getChildren().add(createFileItem(file));
                    }
                }

                if (btnBack != null) {
                    btnBack.setDisable(currentDirectoryId == null);
                }
            });
        });
    }

    // --- TẠO GIAO DIỆN FOLDER ---
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
        vbox.setStyle("-fx-background-radius: 5; -fx-cursor: hand;");
        vbox.setOnMouseEntered(e -> vbox.setStyle("-fx-background-color: #e3f2fd; -fx-background-radius: 5; -fx-cursor: hand;"));
        vbox.setOnMouseExited(e -> vbox.setStyle("-fx-background-color: transparent;"));

        // Click đúp để vào thư mục
        vbox.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                if (isSearching) {
                    if (searchField != null) searchField.clear();
                    isSearching = false;
                }
                historyStack.push(currentDirectoryId == null ? -1L : currentDirectoryId);
                currentDirectoryId = dir.id;
                loadFiles();
            }
        });

        // --- MENU CHUỘT PHẢI CHO FOLDER ---
        ContextMenu cm = new ContextMenu();

        // 1. Chia sẻ
        MenuItem shareItem = new MenuItem("Chia sẻ thư mục");
        shareItem.setOnAction(ev -> handleShareAction(dir.id, dir.name, true));

        // 2. Tải xuống (ĐÃ CÓ HÀM XỬ LÝ Ở DƯỚI)
        MenuItem downloadItem = new MenuItem("Tải xuống (.zip)");
        downloadItem.setOnAction(ev -> handleDownloadFolder(dir.id, dir.name));

        // 3. Xóa
        MenuItem deleteItem = new MenuItem("Xóa thư mục");
        deleteItem.setStyle("-fx-text-fill: red;");
        deleteItem.setOnAction(ev -> handleDeleteFolder(dir.id));

        cm.getItems().addAll(shareItem, downloadItem, new SeparatorMenuItem(), deleteItem);
        vbox.setOnContextMenuRequested(ev -> cm.show(vbox, ev.getScreenX(), ev.getScreenY()));

        return vbox;
    }

    // --- TẠO GIAO DIỆN FILE ---
    private StackPane createFileItem(ListItem.FileDto file) {
        StackPane cardContainer = new StackPane();
        cardContainer.setPrefSize(130, 160);
        cardContainer.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 3, 0, 0, 1);");

        VBox contentBox = new VBox(8);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setPadding(new Insets(25, 5, 5, 5));

        Label nameLbl = new Label(file.originalFilename);
        nameLbl.setMaxWidth(110);
        nameLbl.setWrapText(true);
        nameLbl.setAlignment(Pos.CENTER);
        nameLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-alignment: center;");
        if (file.originalFilename.length() > 30) nameLbl.setText(file.originalFilename.substring(0, 27) + "...");

        ImageView icon = new ImageView(IconHelper.getFileIcon("FILE", file.originalFilename));
        icon.setFitWidth(50); icon.setFitHeight(50);

        contentBox.getChildren().addAll(nameLbl, icon);

        // Action Bar (Star & Menu 3 chấm)
        HBox actionBar = new HBox(0);
        actionBar.setAlignment(Pos.TOP_RIGHT);
        actionBar.setPadding(new Insets(5, 5, 0, 0));
        actionBar.setMaxHeight(140);

        ToggleButton btnStar = new ToggleButton(file.isStarred ? "★" : "☆");
        String starStyle = "-fx-background-color: transparent; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 4 5 0 0; ";
        btnStar.setStyle(starStyle + (file.isStarred ? "-fx-text-fill: #f1c40f;" : "-fx-text-fill: #bdc3c7;"));

        btnStar.setOnAction(e -> {
            e.consume();
            boolean newState = !btnStar.getText().equals("★");
            btnStar.setText(newState ? "★" : "☆");
            btnStar.setStyle(starStyle + (newState ? "-fx-text-fill: #f1c40f;" : "-fx-text-fill: #bdc3c7;"));
            Executors.newSingleThreadExecutor().execute(() -> ClientApiHandler.toggleStar(file.id, false, newState));
        });

        // Menu 3 chấm
        MenuButton menuBtn = new MenuButton("⋮");
        menuBtn.setStyle("-fx-background-color: transparent; -fx-font-size: 16px; -fx-padding: 0; -fx-cursor: hand;");

        MenuItem itemShare = new MenuItem("Chia sẻ");
        itemShare.setOnAction(e -> handleShareAction(file.id, file.originalFilename, false));

        MenuItem itemDownload = new MenuItem("Tải xuống");
        itemDownload.setOnAction(e -> handleDownloadFile(file.id, file.originalFilename));

        MenuItem itemDelete = new MenuItem("Xóa");
        itemDelete.setStyle("-fx-text-fill: red;");
        itemDelete.setOnAction(e -> handleDeleteFile(file.id));

        menuBtn.getItems().addAll(itemShare, itemDownload, new SeparatorMenuItem(), itemDelete);

        actionBar.getChildren().addAll(btnStar, menuBtn);
        actionBar.setVisible(false);

        // Hiệu ứng Hover
        cardContainer.setOnMouseEntered(e -> {
            cardContainer.setStyle("-fx-background-color: #f0f8ff; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 5, 0, 0, 2);");
            actionBar.setVisible(true);
        });

        cardContainer.setOnMouseExited(e -> {
            if (menuBtn.isShowing()) return;
            cardContainer.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 3, 0, 0, 1);");
            if (!btnStar.getText().equals("★")) actionBar.setVisible(false);
            else menuBtn.setVisible(false);
        });

        // Click đúp mở file
        cardContainer.setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.getTarget() instanceof Button || mouseEvent.getTarget() instanceof MenuButton ||
                    (mouseEvent.getTarget() instanceof Node && ((Node)mouseEvent.getTarget()).getParent() instanceof MenuButton)) {
                return;
            }
            if (mouseEvent.getButton() == MouseButton.PRIMARY && mouseEvent.getClickCount() == 2) {
                handleOpenFile(file.id, file.originalFilename);
            }
        });

        cardContainer.getChildren().addAll(contentBox, actionBar);
        return cardContainer;
    }

    // --- CÁC HÀM XỬ LÝ (ACTIONS) ---

    private void handleShareAction(Long itemId, String itemName, boolean isFolder) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Chia sẻ dữ liệu");
        dialog.setHeaderText("Chia sẻ " + (isFolder ? "thư mục" : "file") + ": " + itemName);
        dialog.setContentText("Nhập email người nhận:");

        dialog.showAndWait().ifPresent(email -> {
            if (email.trim().isEmpty()) {
                showAlert("Cảnh báo", "Vui lòng nhập email!", Alert.AlertType.WARNING);
                return;
            }
            Executors.newSingleThreadExecutor().execute(() -> {
                boolean success = ClientApiHandler.shareItem(email.trim(), itemId, isFolder);
                Platform.runLater(() -> {
                    if (success) showAlert("Thành công", "Đã chia sẻ cho: " + email, Alert.AlertType.INFORMATION);
                    else showAlert("Thất bại", "Không tìm thấy người dùng hoặc lỗi server.", Alert.AlertType.ERROR);
                });
            });
        });
    }

    private void handleBack() {
        if (isSearching) {
            if (searchField != null) searchField.clear();
            isSearching = false;
            loadFiles();
            return;
        }
        if (!historyStack.isEmpty()) {
            Long prev = historyStack.pop();
            currentDirectoryId = (prev == -1L) ? null : prev;
            loadFiles();
        }
    }

    private void handleDeleteFile(Long fileId) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Xóa file này?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                Executors.newSingleThreadExecutor().execute(() -> {
                    if (ClientApiHandler.deleteFile(fileId)) Platform.runLater(this::loadFiles);
                    else Platform.runLater(() -> showAlert("Lỗi", "Xóa thất bại", Alert.AlertType.ERROR));
                });
            }
        });
    }

    private void handleDeleteFolder(Long folderId) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Xóa thư mục và toàn bộ nội dung?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                Executors.newSingleThreadExecutor().execute(() -> {
                    try {
                        java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                                .uri(java.net.URI.create("http://localhost:8080/api/directories/" + folderId))
                                .header("Authorization", "Bearer " + ClientApiHandler.jwtToken)
                                .DELETE().build();
                        java.net.http.HttpClient.newHttpClient().send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
                        Thread.sleep(300);
                    } catch (Exception e) {}
                    Platform.runLater(this::loadFiles);
                });
            }
        });
    }

    private void handleCreateFolder() {
        TextInputDialog dialog = new TextInputDialog("New Folder");
        dialog.setTitle("Tạo thư mục");
        dialog.setHeaderText("Nhập tên thư mục:");
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

    // --- TẢI FILE ---
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
                        Platform.runLater(() -> showAlert("Thành công", "Đã tải file xong.", Alert.AlertType.INFORMATION));
                    } catch (IOException e) {}
                }
            });
        }
    }

    // --- [ĐÂY LÀ HÀM BẠN ĐANG THIẾU] TẢI FOLDER ---
    private void handleDownloadFolder(Long folderId, String folderName) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Lưu thư mục dưới dạng ZIP");

        fc.setInitialFileName(folderName + ".zip");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP Archive", "*.zip"));

        File dest = fc.showSaveDialog(btnNew.getScene().getWindow());

        if (dest != null) {
            Executors.newSingleThreadExecutor().execute(() -> {
                // Gọi API tải folder (ClientApiHandler đã có hàm downloadFolderToTemp)
                File temp = ClientApiHandler.downloadFolderToTemp(folderId, folderName);

                Platform.runLater(() -> {
                    if (temp != null && temp.exists()) {
                        try {
                            java.nio.file.Files.copy(temp.toPath(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            showAlert("Thành công", "Đã tải xuống thư mục:\n" + dest.getAbsolutePath(), Alert.AlertType.INFORMATION);
                        } catch (IOException e) {
                            showAlert("Lỗi", "Không thể lưu file: " + e.getMessage(), Alert.AlertType.ERROR);
                        }
                    } else {
                        showAlert("Lỗi", "Tải thất bại. Có thể thư mục rỗng hoặc lỗi server.", Alert.AlertType.ERROR);
                    }
                });
            });
        }
    }

    // --- MỞ FILE ---
    private void handleOpenFile(Long fileId, String fileName) {
        Executors.newSingleThreadExecutor().execute(() -> {
            File tempFile = ClientApiHandler.downloadFileToTemp(fileId, fileName);

            Platform.runLater(() -> {
                if (tempFile != null && tempFile.exists()) {
                    try {
                        Desktop.getDesktop().open(tempFile);
                    } catch (IOException e) {
                        e.printStackTrace();
                        showAlert("Lỗi", "Không tìm thấy ứng dụng để mở file này.", Alert.AlertType.ERROR);
                    } catch (Exception e) {
                        showAlert("Lỗi", "Không thể mở file: " + e.getMessage(), Alert.AlertType.ERROR);
                    }
                } else {
                    showAlert("Lỗi Tải File", "Không thể tải file từ server.", Alert.AlertType.ERROR);
                }
            });
        });
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}