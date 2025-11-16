package com.example.demo;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;

public class Controller {

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
