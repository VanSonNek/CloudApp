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
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
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

    // --- TẠO GIAO DIỆN FOLDER (ĐÃ XÓA FILES VÀ GB) ---
    private Node createFolderItem(ListItem.DirectoryDto dir) {
        // 1. Container chính
        VBox folderCard = new VBox();
        // Đã giảm chiều cao từ 130 xuống 110 cho gọn
        folderCard.setPrefSize(220, 110);

        // Style: Nền trắng, bo góc 18, bóng đổ nhẹ
        String styleNormal = "-fx-background-color: white; " +
                "-fx-background-radius: 18; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.04), 10, 0, 0, 3); " +
                "-fx-cursor: hand;";

        String styleHover =  "-fx-background-color: white; " +
                "-fx-background-radius: 18; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 15, 0, 0, 5); " +
                "-fx-cursor: hand;";

        folderCard.setStyle(styleNormal);
        folderCard.setPadding(new Insets(15, 20, 15, 20));

        // --- HÀNG 1: ICON VÀ MENU ---
        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);

        // Icon Folder
        ImageView icon = new ImageView();
        try {
            icon.setImage(new Image(getClass().getResourceAsStream("/com/example/demo/imgs/folder.png")));
        } catch (Exception e) {
            e.printStackTrace();
        }
        icon.setFitWidth(36); icon.setFitHeight(36); icon.setPreserveRatio(true);

        Region spacerTop = new Region();
        HBox.setHgrow(spacerTop, Priority.ALWAYS);

        // --- XỬ LÝ MENU 3 CHẤM ---
        Label menuDots = new Label("...");
        menuDots.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #9E9E9E; -fx-cursor: hand; -fx-padding: -5 0 0 0;");

        // Menu chức năng
        ContextMenu contextMenu = new ContextMenu();
        contextMenu.setStyle("-fx-font-size: 13px; -fx-background-radius: 8;");

        MenuItem itemShare = new MenuItem("Chia sẻ");
        itemShare.setOnAction(ev -> handleShareAction(dir.id, dir.name, true));

        MenuItem itemDownload = new MenuItem("Tải xuống (.zip)");
        itemDownload.setOnAction(ev -> handleDownloadFolder(dir.id, dir.name));

        MenuItem itemDelete = new MenuItem("Xóa thư mục");
        itemDelete.setStyle("-fx-text-fill: #E53935;");
        itemDelete.setOnAction(ev -> handleDeleteFolder(dir.id));

        contextMenu.getItems().addAll(itemShare, itemDownload, new SeparatorMenuItem(), itemDelete);

        menuDots.setOnMouseClicked(e -> {
            e.consume();
            contextMenu.show(menuDots, Side.BOTTOM, 0, 0);
        });

        topRow.getChildren().addAll(icon, spacerTop, menuDots);

        // --- HÀNG 2: TÊN FOLDER ---
        Region spacerMid = new Region();
        // Tăng khoảng cách spacer này lên một chút để tên nằm đẹp hơn (tùy chỉnh)
        spacerMid.setPrefHeight(15);

        Label nameLbl = new Label(dir.name);
        nameLbl.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #333;");
        nameLbl.setMaxWidth(180);
        nameLbl.setTextOverrun(OverrunStyle.ELLIPSIS);

        // --- ĐÃ XÓA HÀNG 3 (FILES & GB) ---

        // --- GHÉP CÁC PHẦN LẠI ---
        // Chỉ còn topRow, khoảng cách và tên
        folderCard.getChildren().addAll(topRow, spacerMid, nameLbl);

        // --- XỬ LÝ SỰ KIỆN CHUNG ---
        folderCard.setOnMouseEntered(e -> folderCard.setStyle(styleHover));
        folderCard.setOnMouseExited(e -> {
            if (!contextMenu.isShowing()) folderCard.setStyle(styleNormal);
        });

        folderCard.setOnMouseClicked(e -> {
            if (e.getTarget() == menuDots || (e.getTarget() instanceof Node && ((Node)e.getTarget()).getParent() == menuDots)) {
                return;
            }
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                if (isSearching) { if (searchField != null) searchField.clear(); isSearching = false; }
                historyStack.push(currentDirectoryId == null ? -1L : currentDirectoryId);
                currentDirectoryId = dir.id;
                loadFiles();
            }
        });

        return folderCard;
    }
    // --- TẠO GIAO DIỆN FILE (FIX LỖI 2 MŨI TÊN) ---
    private Node createFileItem(ListItem.FileDto file) {
        VBox card = new VBox();
        card.setPrefSize(220, 180);

        // Style chung
        String styleNormal = "-fx-background-color: transparent; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 5); -fx-cursor: hand;";
        String styleHover = "-fx-background-color: transparent; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 15, 0, 0, 8); -fx-cursor: hand;";

        card.setStyle(styleNormal);

        // --- PHẦN 1: THUMBNAIL ---
        StackPane thumbnail = new StackPane();
        thumbnail.setPrefHeight(130);
        thumbnail.setMinHeight(130);
        thumbnail.setStyle("-fx-background-color: #FFF8E1; -fx-background-radius: 18 18 0 0;"); // Màu vàng nhạt

        ImageView mainIcon = new ImageView(IconHelper.getFileIcon("FILE", file.originalFilename));
        mainIcon.setFitWidth(50); mainIcon.setFitHeight(50); mainIcon.setPreserveRatio(true);
        thumbnail.getChildren().add(mainIcon);

        // --- PHẦN 2: FOOTER ---
        HBox footer = new HBox(5);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(10, 10, 10, 15));
        footer.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 18 18;");
        footer.setPrefHeight(50);

        // Icon nhỏ
        ImageView smallIcon = new ImageView(IconHelper.getFileIcon("FILE", file.originalFilename));
        smallIcon.setFitWidth(16); smallIcon.setFitHeight(16);

        // Tên file
        Label nameLbl = new Label(file.originalFilename);
        nameLbl.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #333;");
        nameLbl.setMaxWidth(110);
        nameLbl.setTextOverrun(OverrunStyle.ELLIPSIS);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // --- A. NÚT NGÔI SAO (STAR) ---
        ToggleButton btnStar = new ToggleButton(file.isStarred ? "★" : "☆");
        String starStyle = "-fx-background-color: transparent; -fx-padding: 0; -fx-font-size: 18px; -fx-cursor: hand; ";
        btnStar.setStyle(starStyle + (file.isStarred ? "-fx-text-fill: #FBC02D;" : "-fx-text-fill: #BDBDBD;"));

        btnStar.setOnAction(e -> {
            e.consume();
            boolean newState = !btnStar.getText().equals("★");
            btnStar.setText(newState ? "★" : "☆");
            btnStar.setStyle(starStyle + (newState ? "-fx-text-fill: #FBC02D;" : "-fx-text-fill: #BDBDBD;"));
            Executors.newSingleThreadExecutor().execute(() -> ClientApiHandler.toggleStar(file.id, false, newState));
        });

        // --- B. NÚT MENU MŨI TÊN (SỬA LẠI DÙNG LABEL + CONTEXT MENU) ---
        // Thay vì MenuButton, dùng Label để không bị dính mũi tên mặc định của hệ thống
        Label menuLabel = new Label("▼");
        menuLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #757575; -fx-cursor: hand; -fx-padding: 0 0 0 5;");

        // Tạo ContextMenu chứa các chức năng
        ContextMenu contextMenu = new ContextMenu();

        MenuItem itemShare = new MenuItem("Chia sẻ");
        itemShare.setOnAction(e -> handleShareAction(file.id, file.originalFilename, false));

        MenuItem itemDownload = new MenuItem("Tải xuống");
        itemDownload.setOnAction(e -> handleDownloadFile(file.id, file.originalFilename));

        MenuItem itemDelete = new MenuItem("Xóa");
        itemDelete.setStyle("-fx-text-fill: red;");
        itemDelete.setOnAction(e -> handleDeleteFile(file.id));

        contextMenu.getItems().addAll(itemShare, itemDownload, new SeparatorMenuItem(), itemDelete);

        // Sự kiện khi nhấn vào mũi tên "▼"
        menuLabel.setOnMouseClicked(e -> {
            e.consume(); // Chặn không cho click lan ra thẻ (không mở file)
            contextMenu.show(menuLabel, Side.BOTTOM, 0, 0); // Hiện menu ngay dưới mũi tên
        });

        // Thêm vào Footer
        footer.getChildren().addAll(smallIcon, nameLbl, spacer, btnStar, menuLabel);

        // Ghép vào Card
        card.getChildren().addAll(thumbnail, footer);

        // --- XỬ LÝ SỰ KIỆN CHUNG ---
        card.setOnMouseEntered(e -> card.setStyle(styleHover));
        card.setOnMouseExited(e -> {
            if (!contextMenu.isShowing()) card.setStyle(styleNormal);
        });

        card.setOnMouseClicked(mouseEvent -> {
            Node target = (Node) mouseEvent.getTarget();
            // Kiểm tra xem có click trúng nút Star, MenuLabel hay không
            if (target instanceof Button || target instanceof ToggleButton || target instanceof Label && target == menuLabel ||
                    (target.getParent() instanceof ToggleButton)) {
                return;
            }

            if (mouseEvent.getButton() == MouseButton.PRIMARY && mouseEvent.getClickCount() == 2) {
                handleOpenFile(file.id, file.originalFilename);
            }
        });

        return card;
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