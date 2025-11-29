package com.example.demo.controller;

import java.util.concurrent.Executors;

import com.example.demo.ClientApiHandler;
import com.example.demo.Main;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;

public class LoginController {

    private Main mainApp;

    @FXML private TextField txtUsername;   // Đảm bảo FXML có fx:id="txtUsername"
    @FXML private PasswordField txtPassword; // Đảm bảo FXML có fx:id="txtPassword"
    @FXML private Label lblError;          // Label để hiện lỗi (nếu có trong FXML)

    // Liên kết với Main để chuyển cảnh
    public void setMainApp(Main mainApp) {
        this.mainApp = mainApp;
    }

    /**
     * Sự kiện khi nhấn nút Login
     */
    @FXML
    public void login() {
        // 1. Lấy dữ liệu
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();

        // 2. Validate cơ bản
        if (username.isEmpty() || password.isEmpty()) {
            showError("Vui lòng nhập Username và Password!");
            return;
        }

        // Reset thông báo lỗi đang hiện (nếu có)
        showError(""); 
        System.out.println("Dang dang nhap voi user: " + username);

        // 3. Gọi API Login (Chạy luồng riêng)
        Executors.newSingleThreadExecutor().execute(() -> {
            boolean success = ClientApiHandler.login(username, password);

            // 4. Cập nhật UI (Quay lại luồng JavaFX)
            Platform.runLater(() -> {
                if (success) {
                    System.out.println("✅ Đăng nhập thành công!");
                    if (mainApp != null) {
                        mainApp.showDashboardScene(); // Chuyển sang màn hình chính
                    }
                } else {
                    showError("Sai tên đăng nhập hoặc mật khẩu!");
                }
            });
        });
    }

    /**
     * Sự kiện khi nhấn nút Create Account / Sign Up
     */
    @FXML
    public void createAccount() {
        if (mainApp != null) {
            mainApp.showRegisterScene();
        }
    }

    // Hàm hiển thị lỗi an toàn (tránh NullPointerException nếu chưa có Label)
    private void showError(String message) {
        if (lblError != null) {
            lblError.setText(message);
            lblError.setTextFill(Color.RED);
            lblError.setVisible(true);
        } else {
            // Nếu chưa có Label trong giao diện thì in ra Console đỡ
            if (!message.isEmpty()) {
                System.err.println("Login Error: " + message);
            }
        }
    }
}