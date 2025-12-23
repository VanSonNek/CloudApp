package com.example.demo.controller;

import com.example.demo.ClientApiHandler;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ResourceBundle;

public class AccountController implements Initializable {

    @FXML private Label fullNameLabel;
    @FXML private Label emailLabel;

    // --- CÁC BIẾN MỚI THÊM ---
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField; // Tạm để đó, dù backend chưa có
    @FXML private DatePicker dobPicker;
    @FXML private ComboBox<String> countryBox;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Init dữ liệu mẫu cho ComboBox quốc tịch (nếu cần)
        countryBox.setItems(FXCollections.observableArrayList("Vietnam", "USA", "Japan", "Korea"));

        loadUserProfile();
    }

    private void loadUserProfile() {
        new Thread(() -> {
            ClientApiHandler.UserProfileDto user = ClientApiHandler.getUserProfile();

            if (user != null) {
                Platform.runLater(() -> {
                    // 1. Hiển thị Header
                    fullNameLabel.setText(user.username != null ? user.username : "Unknown");
                    emailLabel.setText(user.email);

                    // 2. Điền vào Form
                    if (firstNameField != null) {
                        firstNameField.setText(user.username); // Dùng username làm First Name
                    }

                    // 3. Xử lý Quốc tịch
                    if (countryBox != null && user.nationality != null) {
                        countryBox.setValue(user.nationality);
                    }

                    // 4. Xử lý Ngày sinh (Convert String -> LocalDate)
                    if (dobPicker != null && user.dateOfBirth != null) {
                        try {
                            // Backend trả về dạng "yyyy-MM-dd" (chuẩn ISO)
                            LocalDate dob = LocalDate.parse(user.dateOfBirth);
                            dobPicker.setValue(dob);
                        } catch (DateTimeParseException e) {
                            System.err.println("Lỗi parse ngày sinh: " + e.getMessage());
                        }
                    }
                });
            }
        }).start();
    }
}