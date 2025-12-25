package com.example.demo.controller;

import com.example.demo.ClientApiHandler;
import com.example.demo.ClientApiHandler.ShareResponse;
import com.example.demo.ClientApiHandler.DirectoryContentResponse;
import com.example.demo.ListItem;
import com.example.demo.util.IconHelper;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.Executors;

public class ShareController {

    @FXML private ListView<ShareResponse> shareListView;
    @FXML private TextField searchField;
    @FXML private Button btnBack;

    private ObservableList<ShareResponse> masterData = FXCollections.observableArrayList();

    // Biến quản lý lịch sử duyệt Folder
    private Stack<Long> folderHistory = new Stack<>();
    private Long currentFolderId = null; // null = Root
    private boolean isAtRoot = true;

    @FXML
    public void initialize() {
        shareListView.setCellFactory(param -> new ShareListCell());

        // Cấu hình nút Back
        if (btnBack != null) {
            btnBack.setDisable(true);
            btnBack.setOnAction(e -> handleBack());
        }

        // Cấu hình tìm kiếm
        FilteredList<ShareResponse> filteredData = new FilteredList<>(masterData, p -> true);
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                filteredData.setPredicate(item -> {
                    if (newVal == null || newVal.isEmpty()) return true;
                    String lower = newVal.toLowerCase();
                    return (item.itemName != null && item.itemName.toLowerCase().contains(lower)) ||
                            (item.itemType != null && item.itemType.toLowerCase().contains(lower));
                });
            });
        }
        // Gán danh sách đã lọc vào ListView (khiến ListView không thể clear trực tiếp)
        shareListView.setItems(filteredData);

        // Tải dữ liệu ban đầu
        loadData();
    }

    // --- HÀM TẢI DỮ LIỆU ---
    private void loadData() {
        // [SỬA LỖI] Không xóa ListView trực tiếp, mà xóa dữ liệu gốc
        masterData.clear();

        Executors.newSingleThreadExecutor().execute(() -> {
            List<ShareResponse> displayList = new ArrayList<>();

            if (isAtRoot) {
                // 1. Ở trang gốc -> Lấy danh sách Share
                List<ShareResponse> sharedFiles = ClientApiHandler.getSharedFiles();
                if (sharedFiles != null) displayList.addAll(sharedFiles);
            } else {
                // 2. Trong folder -> Lấy nội dung folder
                DirectoryContentResponse content = ClientApiHandler.getDirectoryContent(currentFolderId);
                if (content != null) {
                    if (content.directories != null) {
                        for (ListItem.DirectoryDto dir : content.directories) {
                            displayList.add(convertToShareResponse(dir));
                        }
                    }
                    if (content.files != null) {
                        for (ListItem.FileDto file : content.files) {
                            displayList.add(convertToShareResponse(file));
                        }
                    }
                }
            }

            Platform.runLater(() -> {
                masterData.setAll(displayList);
                // Cập nhật trạng thái nút Back
                if (btnBack != null) btnBack.setDisable(isAtRoot);
            });
        });
    }

    // --- HELPER CONVERT DTO ---
    private ShareResponse convertToShareResponse(ListItem.DirectoryDto dir) {
        ShareResponse item = new ShareResponse();
        item.shareId = 0L;
        item.itemId = dir.id;
        item.itemName = dir.name;
        item.itemType = "FOLDER";
        item.senderEmail = "...";
        item.sharedDate = "";
        return item;
    }

    private ShareResponse convertToShareResponse(ListItem.FileDto file) {
        ShareResponse item = new ShareResponse();
        item.shareId = 0L;
        item.itemId = file.id;
        item.itemName = file.originalFilename;
        item.itemType = "FILE";
        item.senderEmail = "...";
        item.sharedDate = file.uploadDate != null ? file.uploadDate.toString() : "";
        return item;
    }

    // --- NAVIGATION ACTIONS ---

    private void handleItemClick(ShareResponse item) {
        if ("FOLDER".equals(item.itemType)) {
            handleOpenFolder(item);
        } else {
            handleOpenFile(item);
        }
    }

    private void handleOpenFolder(ShareResponse item) {
        // Push trạng thái hiện tại vào Stack trước khi đi tiếp
        if (isAtRoot) {
            folderHistory.push(null);
        } else {
            folderHistory.push(currentFolderId);
        }

        // Chuyển trạng thái
        isAtRoot = false;
        currentFolderId = item.itemId;

        loadData();
    }

    private void handleBack() {
        if (folderHistory.isEmpty()) return;

        Long prevId = folderHistory.pop();
        currentFolderId = prevId;

        // Nếu lấy ra null nghĩa là về lại Root
        isAtRoot = (currentFolderId == null);

        loadData();
    }

    // --- FILE ACTIONS ---

    private void handleOpenFile(ShareResponse item) {
        Executors.newSingleThreadExecutor().execute(() -> {
            File temp = ClientApiHandler.downloadFileToTemp(item.itemId, item.itemName);
            Platform.runLater(() -> {
                if (temp != null && temp.exists()) {
                    try { Desktop.getDesktop().open(temp); }
                    catch (Exception e) { new Alert(Alert.AlertType.ERROR, "Không thể mở file.").show(); }
                }
            });
        });
    }

    private void handleDownload(ShareResponse item) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Lưu " + (item.itemType.equals("FOLDER") ? "thư mục" : "file"));
        String suggestName = item.itemName + ("FOLDER".equals(item.itemType) ? ".zip" : "");
        fileChooser.setInitialFileName(suggestName);

        if ("FOLDER".equals(item.itemType))
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Zip Archive", "*.zip"));
        else
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*.*"));

        File destFile = fileChooser.showSaveDialog(shareListView.getScene().getWindow());
        if (destFile != null) {
            Executors.newSingleThreadExecutor().execute(() -> {
                File temp = "FOLDER".equals(item.itemType) ?
                        ClientApiHandler.downloadFolderToTemp(item.itemId, item.itemName) :
                        ClientApiHandler.downloadFileToTemp(item.itemId, item.itemName);

                Platform.runLater(() -> {
                    if (temp != null) {
                        try {
                            Files.copy(temp.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            new Alert(Alert.AlertType.INFORMATION, "Tải thành công!").show();
                        } catch (IOException e) { e.printStackTrace(); }
                    } else {
                        new Alert(Alert.AlertType.ERROR, "Lỗi tải file từ server").show();
                    }
                });
            });
        }
    }

    private void handleViewContent(ShareResponse item) {
        // Logic xem trước file (nếu cần)
        handleOpenFile(item);
    }

    // ================= CLASS GIAO DIỆN TỪNG DÒNG (ĐÃ NÂNG CẤP) =================
    private class ShareListCell extends ListCell<ShareResponse> {
        @Override
        protected void updateItem(ShareResponse item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                // Reset style để không bị lưu style cũ
                setStyle("-fx-background-color: transparent;");
                setContextMenu(null);
            } else {
                // 1. Tạo Grid Container (Card)
                GridPane grid = new GridPane();
                grid.getStyleClass().add("share-item-card"); // Class CSS quan trọng
                grid.setMinHeight(60); // Chiều cao cố định cho đẹp
                grid.setAlignment(Pos.CENTER_LEFT);

                // 2. Thiết lập cột (Phần trăm phải khớp với Header bên FXML)
                ColumnConstraints col1 = new ColumnConstraints(); col1.setPercentWidth(50);
                ColumnConstraints col2 = new ColumnConstraints(); col2.setPercentWidth(25);
                ColumnConstraints col3 = new ColumnConstraints(); col3.setPercentWidth(15);
                ColumnConstraints col4 = new ColumnConstraints(); col4.setPercentWidth(10); col4.setHalignment(javafx.geometry.HPos.CENTER);

                grid.getColumnConstraints().addAll(col1, col2, col3, col4);

                // --- CỘT 1: ICON + TÊN FILE ---
                HBox nameBox = new HBox(15);
                nameBox.setAlignment(Pos.CENTER_LEFT);

                ImageView icon = new ImageView();
                boolean isFolder = "FOLDER".equals(item.itemType);
                try {
                    // Logic lấy icon (giữ nguyên logic của bạn)
                    if (isFolder) icon.setImage(new Image(getClass().getResourceAsStream("/icons/folder.png"))); // Đảm bảo đường dẫn đúng
                    else icon.setImage(IconHelper.getFileIcon("FILE", item.itemName));
                } catch (Exception e) {
                    // Fallback nếu lỗi ảnh
                }
                icon.setFitWidth(32); icon.setFitHeight(32); // Icon to hơn chút cho đẹp

                Label nameLbl = new Label(item.itemName);
                nameLbl.getStyleClass().add("primary-text"); // CSS Text đậm

                nameBox.getChildren().addAll(icon, nameLbl);

                // --- CỘT 2: NGƯỜI GỬI ---
                // Cắt ngắn email nếu quá dài
                String senderDisplay = item.senderEmail;
                if (senderDisplay != null && senderDisplay.length() > 25) {
                    senderDisplay = senderDisplay.substring(0, 22) + "...";
                }
                Label sharerLbl = new Label(senderDisplay);
                sharerLbl.getStyleClass().add("secondary-text"); // CSS Text nhạt

                // --- CỘT 3: NGÀY THÁNG ---
                Label dateLbl = new Label(item.sharedDate);
                dateLbl.getStyleClass().add("secondary-text");

                // --- CỘT 4: LOẠI (BADGE) ---
                Label typeBadge = new Label(isFolder ? "FOLDER" : "FILE");
                // Class CSS khác nhau cho màu sắc khác nhau
                typeBadge.getStyleClass().add(isFolder ? "badge-folder" : "badge-file");

                // Thêm vào Grid
                grid.add(nameBox, 0, 0);
                grid.add(sharerLbl, 1, 0);
                grid.add(dateLbl, 2, 0);
                grid.add(typeBadge, 3, 0);

                setGraphic(grid);

                // --- CONTEXT MENU (Giữ nguyên logic cũ) ---
                ContextMenu cm = new ContextMenu();
                MenuItem openItem = new MenuItem("Open");
                openItem.setOnAction(e -> handleItemClick(item));
                MenuItem downloadItem = new MenuItem("Download");
                downloadItem.setOnAction(e -> handleDownload(item));

                if (!isFolder) {
                    MenuItem viewItem = new MenuItem("Quick View");
                    viewItem.setOnAction(e -> handleViewContent(item));
                    cm.getItems().add(viewItem);
                }
                cm.getItems().addAll(openItem, downloadItem);
                setContextMenu(cm);

                setOnMouseClicked(e -> {
                    if (e.getClickCount() == 2 && !isEmpty()) handleItemClick(item);
                });
            }
        }}
}