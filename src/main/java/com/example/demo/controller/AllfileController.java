package com.example.demo.controller;

import java.io.File;
import java.util.Optional;

import com.example.demo.ClientApiHandler;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Popup;
import javafx.stage.Window; // <<< THÊM: Dùng để chọn thư mục

import javax.swing.text.html.ImageView;

public class AllfileController {

    @FXML private Button btnNew;
    @FXML private javafx.scene.image.ImageView avatarBtn;


    // Tạm thời, ID thư mục hiện tại là ROOT (null)
    private final Long currentDirectoryId = null; 

    @FXML
    public void initialize() {

        System.out.println("Allfile content loaded!");
        setupNewMenu();
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

        // 1. Khởi tạo Popup và Container
        Popup popup = new Popup();
        popup.setAutoHide(true);
        VBox box = new VBox();
        box.setSpacing(5);
        box.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
        box.setPadding(new Insets(10));
        box.setEffect(new javafx.scene.effect.DropShadow(10, Color.rgb(0, 0, 0, 0.18)));

        // 2. Tạo các dòng Menu
        VBox newFolder = createMenuRow("New Folder");
        VBox uploadFile = createMenuRow("Upload File");
        VBox uploadFolder = createMenuRow("Upload Folder");

        // 3. Thêm các dòng vào container và thêm container vào Popup
        box.getChildren().addAll(newFolder, uploadFile, uploadFolder);
        popup.getContent().add(box);

        // 4. Thiết lập sự kiện khi bấm nút New (Show Popup)
        btnNew.setOnAction(e -> {
            popup.show(btnNew,
                    btnNew.localToScreen(btnNew.getBoundsInLocal()).getMinX(),
                    btnNew.localToScreen(btnNew.getBoundsInLocal()).getMaxY() + 5
            );
        });
        
        // 5. Thiết lập sự kiện click cho từng dòng menu
        newFolder.setOnMouseClicked(e -> {
            popup.hide();
            showNewFolderDialog(); 
        });

        uploadFile.setOnMouseClicked(e -> {
            popup.hide();
            handleUploadFile(); 
        });

        // <<< CẬP NHẬT: Xử lý Upload Folder
        uploadFolder.setOnMouseClicked(e -> {
            popup.hide();
            handleUploadFolder(); 
        });
    }

    /** Tạo dòng menu với hiệu ứng hover */
    private VBox createMenuRow(String title) {
        HBox row = new HBox(10);
        row.setPadding(new Insets(10));
        row.setCursor(Cursor.HAND);

        Label label = new Label(title);
        label.setStyle("-fx-font-size: 14px;");

        row.getChildren().add(label);

        row.setOnMouseEntered(e ->
                row.setStyle("-fx-background-color: #EEF6FF; -fx-background-radius: 6;")
        );
        row.setOnMouseExited(e ->
                row.setStyle("-fx-background-color: transparent;")
        );

        VBox wrapper = new VBox(row);
        return wrapper;
    }

    // Xử lý tạo thư mục mới
    private void showNewFolderDialog() {
        if (!ClientApiHandler.isAuthenticated()) {
            System.err.println("Lỗi: Vui lòng đăng nhập trước khi tạo thư mục.");
            return;
        }
        
        TextInputDialog dialog = new TextInputDialog("Thư mục mới");
        dialog.setTitle("Tạo Thư mục Mới");
        dialog.setHeaderText("Nhập tên thư mục bạn muốn tạo:");
        dialog.setContentText("Tên thư mục:");

        Optional<String> result = dialog.showAndWait();
        
        result.ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                ClientApiHandler.createFolder(name.trim(), currentDirectoryId);
            } else {
                System.out.println("Tên thư mục không được để trống.");
            }
        });
    }

    // Xử lý tải file
    private void handleUploadFile() {
        if (!ClientApiHandler.isAuthenticated()) {
            System.err.println("Lỗi: Vui lòng đăng nhập trước khi tải file.");
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn File để tải lên");
        
        Window ownerWindow = btnNew.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(ownerWindow);
        
        if (selectedFile != null) {
            // Gọi hàm upload file
            ClientApiHandler.uploadFile(selectedFile, currentDirectoryId);
        } else {
            System.out.println("Người dùng đã hủy chọn file.");
        }
    }

    // <<< THÊM: Xử lý tải lên thư mục
    private void handleUploadFolder() {
        if (!ClientApiHandler.isAuthenticated()) {
            System.err.println("Lỗi: Vui lòng đăng nhập trước khi tải thư mục.");
            return;
        }

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Chọn Thư mục để tải lên");

        Window ownerWindow = btnNew.getScene().getWindow();
        File selectedDirectory = directoryChooser.showDialog(ownerWindow);

        if (selectedDirectory != null) {
            System.out.println("Bắt đầu tải lên thư mục: " + selectedDirectory.getAbsolutePath());
            
            // Gọi hàm upload directory trong ClientApiHandler
            ClientApiHandler.uploadDirectory(selectedDirectory, currentDirectoryId);
            
        } else {
            System.out.println("Người dùng đã hủy chọn thư mục.");
        }
    }
}