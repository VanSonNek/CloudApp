package com.example.demo.controller;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Executors;

import com.example.demo.ClientApiHandler;
import com.example.demo.Main; // Import Main để chuyển Scene
import com.google.gson.Gson;

import javafx.application.Platform; // Import Platform để chuyển đổi UI Thread
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField; 

class LoginRequest {
    public String username;
    public String password;
    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }
}

class LoginResponse {
    public String username;
    public String password;
}

public class LoginController {

        @FXML
        private TextField txtUsername;

        @FXML
        private PasswordField txtPassword;
        
        private Main mainApp; // Tham chiếu đến Main Application
        private static final String SERVER_URL = "http://localhost:8080";
        private final Gson gson = new Gson();
        
        /** Thiết lập liên kết đến Main Application */
        public void setMainApp(Main mainApp) {
            this.mainApp = mainApp;
        }

        public void login() {
            String username = txtUsername.getText();
            String password = txtPassword.getText();

            String jsonBody = gson.toJson(new LoginRequest(username, password));

            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    HttpClient client = HttpClient.newHttpClient();

                    HttpRequest loginRequest = HttpRequest.newBuilder()
                            .uri(URI.create(SERVER_URL + "/auth/login")) 
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                            .build();

                    HttpResponse<String> loginResponse = client.send(loginRequest, HttpResponse.BodyHandlers.ofString());

                    if (loginResponse.statusCode() == 200) {
                        System.out.println("--- Đăng nhập thành công! ---");

                        // <<< THAY ĐỔI QUAN TRỌNG: LƯU TRỮ THÔNG TIN ĐĂNG NHẬP cho Basic Auth
                        ClientApiHandler.setCredentials(username, password); 

                        // Kiểm tra kết nối API đầu tiên có xác thực (tùy chọn)
                        ClientApiHandler.fetchDataAndPrint();
                        
                        // <<< CHUYỂN SCENE TRÊN UI THREAD
                        if (mainApp != null) {
                             Platform.runLater(() -> {
                                 mainApp.showDashboardScene(); 
                             });
                        }

                    } else {
                        System.err.println("--- Đăng nhập thất bại. Status: " + loginResponse.statusCode() + " ---");
                        // Thêm logic thông báo lỗi trên UI
                    }
                } catch (IOException | InterruptedException e) {
                    System.err.println("Lỗi kết nối hoặc xử lý yêu cầu đăng nhập.");
                    e.printStackTrace();
                    // Thêm logic thông báo lỗi trên UI
                }
            });
        }

    public void createAccount() {
        if (mainApp != null) {
            mainApp.showRegisterScene();
        }
    }
}