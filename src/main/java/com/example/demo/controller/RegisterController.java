package com.example.demo.controller;

import java.util.concurrent.Executors;

import com.example.demo.ClientApiHandler;
import com.example.demo.Main;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController {

    private Main mainApp;

    // Khai báo đúng 4 trường như trong giao diện
    @FXML private TextField txtUsername;         // Nhập Username
    @FXML private TextField txtEmail;            // Nhập Email
    @FXML private PasswordField txtPassword;     // Nhập Password
    @FXML private PasswordField txtConfirmPassword; // Nhập lại Password (Confirm)

    public void setMainApp(Main mainApp) {
        this.mainApp = mainApp;
    }

    @FXML
    public void register() {
        // 1. Lấy dữ liệu từ 4 ô
        String username = txtUsername.getText().trim();
        String email = txtEmail.getText().trim();
        String password = txtPassword.getText();
        String confirm = txtConfirmPassword.getText();

        // 2. Validate (Kiểm tra rỗng)
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            showAlert("Thiếu thông tin", "Vui lòng nhập đầy đủ cả 4 trường!", Alert.AlertType.WARNING);
            return;
        }

        // 3. Validate (Kiểm tra mật khẩu trùng khớp)
        if (!password.equals(confirm)) {
            showAlert("Lỗi mật khẩu", "Mật khẩu xác nhận không trùng khớp!", Alert.AlertType.ERROR);
            return;
        }

        // 4. Gửi 3 thông tin quan trọng lên Server (Username, Email, Password)
        Executors.newSingleThreadExecutor().execute(() -> {
            boolean success = ClientApiHandler.register(username, email, password);

            Platform.runLater(() -> {
                if (success) {
                    showAlert("Thành công", "Đăng ký thành công! Vui lòng đăng nhập.", Alert.AlertType.INFORMATION);
                    gotoLogin();
                } else {
                    showAlert("Thất bại", "Email đã tồn tại hoặc lỗi kết nối.", Alert.AlertType.ERROR);
                }
            });
        });
    }

    @FXML
    public void gotoLogin() {
        if (mainApp != null) {
            try {
                mainApp.showLoginScene();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}