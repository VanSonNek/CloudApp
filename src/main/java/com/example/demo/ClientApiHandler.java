package com.example.demo;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

// Request/Response DTOs
class CreateDirectoryRequest {
    public String name;
    public Long parentDirectoryId;
    public CreateDirectoryRequest(String name, Long parentDirectoryId) {
        this.name = name;
        this.parentDirectoryId = parentDirectoryId;
    }
}

class DirectoryContentResponse {
    public List<ListItem.DirectoryDto> directories;
    public List<ListItem.FileDto> files;
}

class LoginRequest {
    public String username;
    public String password;
    public LoginRequest(String username, String password) { this.username = username; this.password = password; }
}

public class ClientApiHandler {

    private static final String SERVER_URL = "http://localhost:8080";
    private static final Gson gson = new Gson();
    private static final HttpClient client = HttpClient.newBuilder().build();

    // ✅ THÊM BIẾN LƯU TOKEN
    private static String jwtToken = null;
    private static boolean authenticated = false;

    public static boolean isAuthenticated() { return authenticated; }

    // Helper tạo Request (SỬA LẠI: Dùng Bearer Token)
    private static HttpRequest.Builder createRequestBuilder(String url) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(SERVER_URL + url));

        if (authenticated && jwtToken != null) {
             // ✅ Gửi Token chuẩn JWT thay vì Basic Auth
             builder.header("Authorization", "Bearer " + jwtToken);
        }
        return builder;
    }

    // --- LOGIN (SỬA LẠI: Lấy Token từ JSON) ---
    public static boolean login(String u, String p) {
        try {
            String jsonInputString = String.format("{\"username\": \"%s\", \"password\": \"%s\"}", u, p);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SERVER_URL + "/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonInputString))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                // ✅ Parse JSON để lấy Token: {"token": "ey..."}
                JsonObject json = gson.fromJson(response.body(), JsonObject.class);
                if (json.has("token")) {
                    jwtToken = json.get("token").getAsString();
                    authenticated = true;
                    System.out.println("🔑 Đã lưu Token: " + jwtToken.substring(0, 15) + "...");
                    return true;
                }
            } else {
                System.err.println("Login Failed: " + response.statusCode());
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // --- REGISTER ---
    public static boolean register(String username, String email, String password) {
        try {
            String jsonInputString = String.format("{\"username\": \"%s\", \"email\": \"%s\", \"password\": \"%s\"}", username, email, password);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SERVER_URL + "/auth/register"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonInputString))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // --- CREATE FOLDER (Async) ---
    public static void createFolder(String name, Long parentDirectoryId) {
        Executors.newSingleThreadExecutor().execute(() -> {
            createFolderSync(name, parentDirectoryId);
        });
    }

    // --- CREATE FOLDER (Sync - Fix lỗi trả về null) ---
    private static Long createFolderSync(String name, Long parentDirectoryId) {
        try {
            CreateDirectoryRequest request = new CreateDirectoryRequest(name, parentDirectoryId);
            HttpRequest httpRequest = createRequestBuilder("/api/directories")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(request)))
                    .build();

            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                JsonObject json = gson.fromJson(response.body(), JsonObject.class);
                return json.get("id").getAsLong();
            } else {
                System.err.println("❌ Lỗi tạo folder '" + name + "': " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // --- LIST FILES ---
    private static DirectoryContentResponse getDirectoryContent(Long directoryId) throws Exception {
        String url = "/api/directories/" + (directoryId == null ? "0" : directoryId);
        
        HttpRequest httpRequest = createRequestBuilder(url)
                .GET()
                .build();

        HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return gson.fromJson(response.body(), DirectoryContentResponse.class);
        } else {
            System.err.println("❌ Lỗi lấy danh sách file: " + response.statusCode());
        }
        return null;
    }

    public static List<ListItem.DirectoryDto> getDirectories(Long parentId) throws Exception {
        DirectoryContentResponse res = getDirectoryContent(parentId);
        return res != null ? res.directories : Collections.emptyList();
    }

    public static List<ListItem.FileDto> getFiles(Long directoryId) throws Exception {
        DirectoryContentResponse res = getDirectoryContent(directoryId);
        return res != null ? res.files : Collections.emptyList();
    }

    // --- UPLOAD FILE ---
    public static void uploadFile(File file, Long directoryId) {
        Executors.newSingleThreadExecutor().execute(() -> {
            uploadFileSync(file, directoryId);
        });
    }

    // ✅ SỬA LỖI UPLOAD: Dùng BodyPublishers.concat
    private static void uploadFileSync(File file, Long directoryId) {
        try {
            String url = "/api/files/upload";
            if (directoryId != null) url += "?parentDirectoryId=" + directoryId;

            String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
            
            String mimeType = Files.probeContentType(file.toPath());
            if (mimeType == null) mimeType = "application/octet-stream";
            
            String header = "--" + boundary + "\r\n" +
                            "Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n" +
                            "Content-Type: " + mimeType + "\r\n\r\n";
            
            String footer = "\r\n--" + boundary + "--\r\n";

            HttpRequest.BodyPublisher body = HttpRequest.BodyPublishers.concat(
                    HttpRequest.BodyPublishers.ofString(header),
                    HttpRequest.BodyPublishers.ofFile(file.toPath()),
                    HttpRequest.BodyPublishers.ofString(footer)
            );

            HttpRequest httpRequest = createRequestBuilder(url)
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(body)
                    .build();

            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            System.out.println("Upload " + file.getName() + ": " + response.statusCode());

        } catch (Exception e) {
            System.err.println("Lỗi upload: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- UPLOAD FOLDER ---
    public static void uploadDirectory(File dir, Long parentServerId) {
        Executors.newSingleThreadExecutor().execute(() -> {
            uploadDirectoryRecursive(dir, parentServerId);
        });
    }

    private static void uploadDirectoryRecursive(File localDir, Long parentServerId) {
        System.out.println("📂 Đang tạo folder: " + localDir.getName());
        Long newServerFolderId = createFolderSync(localDir.getName(), parentServerId);

        if (newServerFolderId == null) {
            System.err.println("❌ Không thể tạo folder " + localDir.getName() + ", dừng upload nhánh này.");
            return;
        }

        File[] files = localDir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile()) {
                    uploadFileSync(f, newServerFolderId);
                } else if (f.isDirectory()) {
                    uploadDirectoryRecursive(f, newServerFolderId);
                }
            }
        }
    }

    public static File downloadFileToTemp(Long fileId, String fileName) {
        try {
            String url = "/api/files/download/" + fileId;
            // Tạo request GET có kèm Token (nếu cần)
            HttpRequest request = createRequestBuilder(url).GET().build();

            // Gửi request và nhận dữ liệu dưới dạng luồng (InputStream)
            HttpResponse<java.io.InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() == 200) {
                // 1. Tách đuôi file (ví dụ .jpg, .docx) để tạo file tạm đúng định dạng
                String extension = "";
                int i = fileName.lastIndexOf('.');
                if (i > 0) {
                    extension = fileName.substring(i);
                } else {
                    extension = ".tmp"; // Mặc định nếu không có đuôi
                }

                // 2. Tạo file tạm trong thư mục Temp của máy tính
                File tempFile = File.createTempFile("skybox_", extension);
                
                // 3. Ghi dữ liệu từ Server vào file tạm này
                java.nio.file.Files.copy(response.body(), tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                
                System.out.println("⬇ Đã tải file về: " + tempFile.getAbsolutePath());
                return tempFile;
            } else {
                System.err.println("❌ Lỗi tải file: Server trả về code " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
