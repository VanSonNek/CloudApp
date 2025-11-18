package com.example.demo.controller;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {
    @FXML
    private TextField txtUsername;

    @FXML
    private PasswordField txtPassword;

    public void login() {
        System.out.println("Username: " + txtUsername.getText());
        System.out.println("Password: " + txtPassword.getText());
    }

    public void createAccount() {
        System.out.println("Đi tới trang đăng ký");
    }
}
