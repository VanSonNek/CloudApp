package com.example.demo.controller;

import com.example.demo.Main;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController {

    private Main mainApp; // dùng để chuyển scene

    @FXML private TextField txtUsername;
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirm;

    /** Gán tham chiếu Main */
    public void setMainApp(Main mainApp) {
        this.mainApp = mainApp;
    }

    /** Xử lý khi nhấn nút Register */
    @FXML
    public void register() {
        String username = txtUsername.getText();
        String email = txtEmail.getText();
        String password = txtPassword.getText();
        String confirm = txtConfirm.getText();

        if (!password.equals(confirm)) {
            System.out.println("❌ Password không trùng khớp!");
            return;
        }

        System.out.println("Đăng ký thành công cho user: " + username);

        // Sau khi đăng ký → chuyển về Login
        if (mainApp != null) {
            try {
                mainApp.showLoginScene();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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


}
