package com.example.demo.controller;

import com.example.demo.ClientApiHandler;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

// SỬA Ở ĐÂY: Dùng javafx.event.ActionEvent thay vì java.awt...
import javafx.event.ActionEvent;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ResourceBundle;
import java.util.Objects;
import javafx.scene.control.*;

public class AccountController implements Initializable {

    @FXML private Label fullNameLabel;
    @FXML private Label emailLabel;

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private DatePicker dobPicker;
    @FXML private ComboBox<String> countryBox;

    // --- HÀM XỬ LÝ ĐĂNG XUẤT ---
    @FXML
    private void handleLogout(ActionEvent event) {
        ClientApiHandler.logout();

        try {
            // 1. Load file FXML đăng nhập
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/login.fxml"));
            Parent root = loader.load();

            // 2. Lấy Stage hiện tại
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            // 3. Tạo Scene mới
            Scene scene = new Scene(root);

            // --- BỔ SUNG QUAN TRỌNG: NẠP CSS ---
            // Giả sử file css của bạn nằm ở /com/example/demo/css/styles.css
            // Bạn hãy đổi đường dẫn bên dưới cho đúng với file css trong dự án của bạn
            String cssPath = "/com/example/demo/css/styles.css"; // Kiểm tra lại đường dẫn này trong project của bạn
            if (getClass().getResource(cssPath) != null) {
                scene.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());
            }
            // ------------------------------------

            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // === THÊM MỚI: Xử lý nút Update ===
    @FXML
    private void handleUpdate(ActionEvent event) {
        // 1. Lấy dữ liệu từ giao diện
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String nationality = countryBox.getValue();
        LocalDate dob = dobPicker.getValue();

        // 2. Validate đơn giản (tùy chọn)
        if (firstName.isEmpty() || lastName.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Warning", "First name and Last name cannot be empty!");
            return;
        }

        // 3. Gọi API cập nhật (chạy trên thread khác để không đơ UI)
        new Thread(() -> {
            String dobString = (dob != null) ? dob.toString() : null; // Chuyển LocalDate sang String yyyy-MM-dd

            boolean success = ClientApiHandler.updateUserProfile(firstName, lastName, nationality, dobString);

            Platform.runLater(() -> {
                if (success) {
                    // Cập nhật lại Label hiển thị tên ngay lập tức
                    fullNameLabel.setText(firstName + " " + lastName);
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Profile updated successfully!");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to update profile. Please try again.");
                }
            });
        }).start();
    }
    // Hàm phụ trợ để hiển thị thông báo
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        countryBox.setItems(FXCollections.observableArrayList("Vietnam", "USA", "Japan", "Korea"));
        loadUserProfile();
    }

    // ... (Phần loadUserProfile giữ nguyên)
    private void loadUserProfile() {
        new Thread(() -> {
            ClientApiHandler.UserProfileDto user = ClientApiHandler.getUserProfile();
            if (user != null) {
                Platform.runLater(() -> {
                    String fullName = (user.username != null ? user.username : "") + " " +
                            (user.lastname != null ? user.lastname : "");
                    fullNameLabel.setText(fullName.trim().isEmpty() ? "Unknown" : fullName.trim());
                    emailLabel.setText(user.email);

                    if (firstNameField != null && user.username != null) firstNameField.setText(user.username);
                    if (lastNameField != null && user.lastname != null) lastNameField.setText(user.lastname);
                    if (countryBox != null && user.nationality != null) countryBox.setValue(user.nationality);
                    if (dobPicker != null && user.dateOfBirth != null) {
                        try {
                            dobPicker.setValue(LocalDate.parse(user.dateOfBirth));
                        } catch (DateTimeParseException e) {
                            System.err.println("Lỗi parse ngày sinh: " + e.getMessage());
                        }
                    }
                });
            }
        }).start();
    }
}