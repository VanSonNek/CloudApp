package com.example.demo.controller;

import java.util.concurrent.Executors;

import com.example.demo.ClientApiHandler;
import com.example.demo.Main; 

import javafx.application.Platform;
import javafx.fxml.FXML; // Thêm import Label
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField; // Thêm import Color
import javafx.scene.paint.Color; // Cần thiết cho việc gọi API bất đồng bộ

/* * ✅ ĐÃ XÓA: LoginRequest và LoginResponse
 * Hai lớp này nên được định nghĩa trong ClientApiHandler.java
 */

public class LoginController {

    @FXML
    private TextField txtUsername;

    @FXML
    private PasswordField txtPassword;
    
    // ✅ THÊM TRƯỜNG NÀY (Cần phải liên kết với FXML)
    @FXML
    private Label lblError; 
    
    private Main mainApp; // Tham chiếu đến Main Application
    
    /** Thiết lập liên kết đến Main Application */
    public void setMainApp(Main mainApp) {
        this.mainApp = mainApp;
    }

    // ✅ PHƯƠNG THỨC XỬ LÝ ĐĂNG NHẬP ĐÃ ĐƯỢC SỬA GỌN
    @FXML
    public void login() {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Vui lòng nhập đầy đủ Tên người dùng và Mật khẩu.");
            return;
        }

        // Xóa thông báo lỗi cũ
        showError(""); 

        // Thực hiện đăng nhập trên một luồng khác để không làm treo UI
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Gọi API Login đã được định nghĩa trong ClientApiHandler
                boolean success = ClientApiHandler.login(username, password);

                // Cập nhật UI trên JavaFX Application Thread
                Platform.runLater(() -> {
                    if (success) {
                        System.out.println("--- Đăng nhập thành công! ---");
                        if (mainApp != null) {
                            mainApp.showDashboardScene(); 
                        }
                    } else {
                        // Thất bại: Đã được xử lý lỗi trong ClientApiHandler (Status 401)
                        showError("Tên người dùng hoặc mật khẩu không đúng.");
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showError("Lỗi kết nối Server.");
                });
            }
        });
    }

    // ✅ PHƯƠNG THỨC HỖ TRỢ HIỂN THỊ LỖI
    private void showError(String message) {
        if (lblError != null) {
            lblError.setText(message);
            lblError.setTextFill(Color.web("#d00000")); // Màu đỏ cho lỗi
        }
    }

    // Giữ nguyên logic chuyển màn hình đăng ký
    public void createAccount() {
        if (mainApp != null) {
            mainApp.showRegisterScene();
        }
    }
}