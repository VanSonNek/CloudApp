package com.example.demo.controller;

import java.io.IOException;
import java.util.concurrent.Executors;

import com.example.demo.ClientApiHandler;
import com.example.demo.Main;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class LoginController {

    private Main mainApp;

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblError;

    public void setMainApp(Main mainApp) {
        this.mainApp = mainApp;
    }

    @FXML
    public void login() {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Vui lòng nhập Username và Password!");
            return;
        }

        showError("");

        // Chạy luồng riêng để gọi API
        Executors.newSingleThreadExecutor().execute(() -> {
            boolean success = ClientApiHandler.login(username, password);

            Platform.runLater(() -> {
                if (success) {
                    System.out.println("✅ Đăng nhập thành công!");

                    // --- SỬA ĐOẠN NÀY ĐỂ KHẮC PHỤC LỖI ---
                    if (mainApp != null) {
                        // Trường hợp 1: Chạy từ Main đầu tiên
                        mainApp.showDashboardScene();
                    } else {
                        // Trường hợp 2: Chạy sau khi Logout (mainApp bị null) -> Tự chuyển cảnh
                        openDashboardDirectly();
                    }
                    // -------------------------------------

                } else {
                    showError("Sai tên đăng nhập hoặc mật khẩu!");
                }
            });
        });
    }

    // Hàm mới để tự mở Dashboard khi không có mainApp
    private void openDashboardDirectly() {
        try {
            // LƯU Ý: Thay đổi tên file fxml bên dưới thành tên file màn hình chính của bạn
            // Ví dụ: DashboardFrame.fxml hoặc dashboard_content.fxml tùy cấu trúc bạn
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/DashboardFrame.fxml"));
            Parent root = loader.load();

            // Lấy Stage hiện tại từ nút hoặc ô nhập liệu
            Stage stage = (Stage) txtUsername.getScene().getWindow();

            Scene scene = new Scene(root);
            // Nếu có CSS cho Dashboard thì thêm vào đây
            // scene.getStylesheets().add(...)

            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showError("Lỗi không tìm thấy màn hình Dashboard!");
        }
    }

    @FXML
    public void createAccount() {
        // Tương tự, xử lý chuyển sang trang đăng ký
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/register.fxml")); // Kiểm tra lại tên file
            Parent root = loader.load();
            Stage stage = (Stage) txtUsername.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        if (lblError != null) {
            lblError.setText(message);
            lblError.setTextFill(Color.RED);
            lblError.setVisible(true);
        }
    }
}