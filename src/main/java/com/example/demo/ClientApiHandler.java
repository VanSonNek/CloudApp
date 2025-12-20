package com.example.demo;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;

public class ClientApiHandler {

    private static final String SERVER_URL = "http://localhost:8080";
    private static final Gson gson = new Gson();
    private static final HttpClient client = HttpClient.newBuilder().build();

    public static String jwtToken = null;

    // --- DTO CLASSES ---

    public static class DirectoryContentResponse {
        public List<ListItem.DirectoryDto> directories;
        public List<ListItem.FileDto> files;
    }

    public static class TrashResponse {
        public Long trashId;
        public String itemName;
        public boolean isFolder;
        public Long size;
        public String deletedDate;
    }

    // ✅ BỔ SUNG: Class ShareResponse bị thiếu
    public static class ShareResponse {
        public Long shareId;
        public String fileName; // Tên file hoặc folder
        public String ownerEmail; // Email người chia sẻ
        public String sharedDate;
        public boolean isFolder;
        public Long fileId;
        public Long folderId;
    }

    public static class DashboardMetrics {
        public Long activeSize = 0L;
        public Long trashSize = 0L;
        public Long transferToday = 0L;
    }

    // --- REQUEST BUILDER ---

    private static HttpRequest.Builder createRequestBuilder(String endpoint) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(SERVER_URL + endpoint));
        if (jwtToken != null) {
            builder.header("Authorization", "Bearer " + jwtToken);
        }
        return builder;
    }

    // --- 1. AUTHENTICATION ---

    public static boolean login(String u, String p) {
        try {
            String json = String.format("{\"username\": \"%s\", \"password\": \"%s\"}", u, p);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SERVER_URL + "/auth/login")) // Đã sửa đúng path api/auth/login
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonObject obj = gson.fromJson(response.body(), JsonObject.class);
                boolean authenticated;
                if (obj.has("accessToken")) {
                    jwtToken = obj.get("accessToken").getAsString();
                    authenticated = true;
                    return true;
                } else if (obj.has("token")) {
                    jwtToken = obj.get("token").getAsString();
                    authenticated = true;
                    return true;
                }
            }
            return false;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public static boolean register(String username, String email, String password) {
        try {
            String json = String.format("{\"username\": \"%s\", \"email\": \"%s\", \"password\": \"%s\"}", username, email, password);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SERVER_URL + "/api/auth/register"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            return client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode() == 200;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    // --- 2. FILE & FOLDER CONTENT ---

    public static DirectoryContentResponse getDirectoryContent(Long directoryId) {
        try {
            String url = "/api/directories/content";
            if (directoryId != null) {
                url += "?directoryId=" + directoryId;
            }
            HttpRequest request = createRequestBuilder(url).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return gson.fromJson(response.body(), DirectoryContentResponse.class);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    // --- 3. UPLOAD & CREATE ---

    public static boolean uploadFile(File file, Long directoryId) {
        try {
            String boundary = "---ContentBoundary" + System.currentTimeMillis();
            String header = "--" + boundary + "\r\n" +
                    "Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n" +
                    "Content-Type: " + Files.probeContentType(file.toPath()) + "\r\n\r\n";
            String footer = "\r\n--" + boundary + "--\r\n";

            byte[] fileBytes = Files.readAllBytes(file.toPath());
            byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);
            byte[] footerBytes = footer.getBytes(StandardCharsets.UTF_8);

            byte[] fullBody = new byte[headerBytes.length + fileBytes.length + footerBytes.length];
            System.arraycopy(headerBytes, 0, fullBody, 0, headerBytes.length);
            System.arraycopy(fileBytes, 0, fullBody, headerBytes.length, fileBytes.length);
            System.arraycopy(footerBytes, 0, fullBody, headerBytes.length + fileBytes.length, footerBytes.length);

            String url = "/api/files/upload";
            if (directoryId != null) url += "?directoryId=" + directoryId;

            HttpRequest request = createRequestBuilder(url)
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(fullBody))
                    .build();

            return client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode() == 200;
        } catch (Exception e) { return false; }
    }

    public static List<ListItem.FileDto> getRecentFiles() {
        try {
            // Gọi API lấy danh sách file gần đây
            HttpRequest req = createRequestBuilder("/api/files/recent").GET().build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());

            if (res.statusCode() == 200) {
                Type listType = new TypeToken<List<ListItem.FileDto>>(){}.getType();
                return gson.fromJson(res.body(), listType);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return Collections.emptyList();
    }

    public static Long createDirectoryAndGetId(String name, Long parentId) {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("name", name);
            if (parentId != null) json.addProperty("parentDirectoryId", parentId);

            HttpRequest req = createRequestBuilder("/api/directories/create")
                    .header("Content-Type", "application/json") // ⚠️ BẮT BUỘC PHẢI CÓ
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(json)))
                    .build();

            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());

            // Log kết quả để debug
            if (res.statusCode() == 200) {
                JsonObject responseJson = gson.fromJson(res.body(), JsonObject.class);

                // Server Spring Boot thường trả về "id", nhưng kiểm tra kỹ
                if (responseJson.has("id")) return responseJson.get("id").getAsLong();
                if (responseJson.has("folderId")) return responseJson.get("folderId").getAsLong();
            } else {
                System.err.println("❌ Lỗi tạo folder: Code " + res.statusCode());
                System.err.println("   Nội dung lỗi: " + res.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void uploadFolderRecursive(File localFolder, Long serverParentId) {
        if (!localFolder.isDirectory()) return;
        Long newServerFolderId = createDirectoryAndGetId(localFolder.getName(), serverParentId);
        if (newServerFolderId == null) return;

        File[] children = localFolder.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) uploadFolderRecursive(child, newServerFolderId);
                else uploadFile(child, newServerFolderId);
            }
        }
    }

    // --- 4. SHARE (CHIA SẺ) ---

    // ✅ BỔ SUNG: Hàm lấy danh sách file được share
    public static List<ShareResponse> getSharedFiles() {
        try {
            HttpRequest req = createRequestBuilder("/api/share/list").GET().build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 200) {
                Type listType = new TypeToken<List<ShareResponse>>(){}.getType();
                return gson.fromJson(res.body(), listType);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return Collections.emptyList();
    }

    // Hàm thực hiện chia sẻ
    public static boolean shareItem(Long itemId, String email, boolean isFolder) {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("email", email);
            if (isFolder) json.addProperty("folderId", itemId);
            else json.addProperty("fileId", itemId);

            HttpRequest req = createRequestBuilder("/api/share/create")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(json)))
                    .build();
            return client.send(req, HttpResponse.BodyHandlers.ofString()).statusCode() == 200;
        } catch (Exception e) { return false; }
    }

    // --- 5. DOWNLOAD & UTILS ---

    // ✅ BỔ SUNG: Hàm download file về thư mục tạm (Dùng để mở hoặc preview)
    public static File downloadFileToTemp(Long fileId, String fileName) {
        try {
            HttpRequest req = createRequestBuilder("/api/files/download/" + fileId).GET().build();
            HttpResponse<InputStream> res = client.send(req, HttpResponse.BodyHandlers.ofInputStream());

            if (res.statusCode() == 200) {
                // Tạo file tạm với prefix, suffix
                String suffix = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf(".")) : ".tmp";
                File tempFile = File.createTempFile("cloud_temp_", suffix);
                Files.copy(res.body(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                return tempFile;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    // --- 6. TRASH & METRICS ---

    public static List<TrashResponse> getTrashItems() {
        try {
            HttpRequest req = createRequestBuilder("/api/trash").GET().build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 200) {
                Type listType = new TypeToken<List<TrashResponse>>(){}.getType();
                return gson.fromJson(res.body(), listType);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return Collections.emptyList();
    }

    public static boolean restoreItem(Long trashId) { // Mặc định cũ, có thể không dùng nữa
        return restoreItem(trashId, false);
    }

    // Hàm restore mới có phân biệt file/folder
    public static boolean restoreItem(Long trashId, boolean isFolder) {
        try {
            // API Server có thể là /api/trash/restore/{id} hoặc cần thêm params
            // Giả sử server tự biết dựa vào ID
            HttpRequest req = createRequestBuilder("/api/trash/restore/" + trashId)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            return client.send(req, HttpResponse.BodyHandlers.ofString()).statusCode() == 200;
        } catch (Exception e) { return false; }
    }

    public static boolean deleteForever(Long trashId) {
        try {
            HttpRequest req = createRequestBuilder("/api/trash/" + trashId)
                    .DELETE().build();
            return client.send(req, HttpResponse.BodyHandlers.ofString()).statusCode() == 200;
        } catch (Exception e) { return false; }
    }

    public static boolean deleteFile(Long fileId) {
        try {
            HttpRequest req = createRequestBuilder("/api/files/" + fileId).DELETE().build();
            return client.send(req, HttpResponse.BodyHandlers.ofString()).statusCode() == 200;
        } catch (Exception e) { return false; }
    }

    public static DashboardMetrics getDashboardMetrics() {
        try {
            HttpRequest req = createRequestBuilder("/api/files/dashboard/metrics").GET().build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 200) {
                return gson.fromJson(res.body(), DashboardMetrics.class);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return new DashboardMetrics();
    }
}