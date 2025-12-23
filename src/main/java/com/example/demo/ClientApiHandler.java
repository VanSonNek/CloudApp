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

    public static class ShareResponse {
        public Long shareId;
        public String itemName;      // Tên file hoặc folder
        public String itemType;      // "FILE" hoặc "FOLDER"
        public Long itemId;          // ID của file hoặc folder
        public String senderEmail;   // Email người gửi
        public String sharedDate;    // Ngày chia sẻ
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
                    .uri(URI.create(SERVER_URL + "/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonObject obj = gson.fromJson(response.body(), JsonObject.class);
                if (obj.has("accessToken")) {
                    jwtToken = obj.get("accessToken").getAsString();
                    return true;
                } else if (obj.has("token")) {
                    jwtToken = obj.get("token").getAsString();
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
                    .uri(URI.create(SERVER_URL + "/auth/register"))
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
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(json)))
                    .build();

            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());

            if (res.statusCode() == 200) {
                JsonObject responseJson = gson.fromJson(res.body(), JsonObject.class);
                if (responseJson.has("id")) return responseJson.get("id").getAsLong();
                if (responseJson.has("folderId")) return responseJson.get("folderId").getAsLong();
            } else {
                System.err.println("❌ Lỗi tạo folder: Code " + res.statusCode());
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

    public static boolean shareItem(String email, Long itemId, boolean isFolder) {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("email", email);
            json.addProperty("itemId", itemId);
            json.addProperty("isFolder", isFolder);

            HttpRequest request = createRequestBuilder("/api/shares/share")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(json)))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<ShareResponse> getSharedFiles() {
        try {
            HttpRequest request = createRequestBuilder("/api/shares/received").GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Type listType = new TypeToken<List<ShareResponse>>(){}.getType();
                return gson.fromJson(response.body(), listType);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    // --- 5. DOWNLOAD & UTILS ---

    public static File downloadFileToTemp(Long fileId, String fileName) {
        try {
            HttpRequest request = createRequestBuilder("/api/files/download/" + fileId).GET().build();
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() == 200) {
                String extension = ".tmp";
                int i = fileName.lastIndexOf('.');
                if (i > 0) extension = fileName.substring(i);

                File tempFile = File.createTempFile("skybox_open_", extension);
                tempFile.deleteOnExit();

                Files.copy(response.body(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                return tempFile;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // [QUAN TRỌNG] Chỉ giữ lại hàm này, XÓA hàm handleDownloadFolder chứa giao diện
    public static File downloadFolderToTemp(Long folderId, String folderName) {
        try {
            HttpRequest request = createRequestBuilder("/api/directories/download/" + folderId).GET().build();
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() == 200) {
                File tempFile = File.createTempFile("skybox_folder_", ".zip");
                tempFile.deleteOnExit();

                Files.copy(response.body(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                return tempFile;
            } else {
                System.out.println("Download Folder Failed: " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    public static boolean restoreItem(Long trashId) {
        return restoreItem(trashId, false);
    }

    public static boolean restoreItem(Long trashId, boolean isFolder) {
        try {
            HttpRequest req = createRequestBuilder("/api/trash/restore/" + trashId)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            return client.send(req, HttpResponse.BodyHandlers.ofString()).statusCode() == 200;
        } catch (Exception e) { return false; }
    }

    public static boolean deleteForever(Long trashId) {
        try {
            HttpRequest req = createRequestBuilder("/api/trash/" + trashId).DELETE().build();
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

    public static List<ListItem.FileDto> getStarredFiles() {
        try {
            HttpRequest req = createRequestBuilder("/api/files/starred").GET().build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 200) {
                Type listType = new TypeToken<List<ListItem.FileDto>>(){}.getType();
                return gson.fromJson(res.body(), listType);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return Collections.emptyList();
    }

    public static boolean toggleStar(Long id, boolean isFolder, boolean starred) {
        try {
            String url = "/api/files/toggle-star?id=" + id + "&isFolder=" + isFolder + "&starred=" + starred;
            HttpRequest req = createRequestBuilder(url).PUT(HttpRequest.BodyPublishers.noBody()).build();
            return client.send(req, HttpResponse.BodyHandlers.ofString()).statusCode() == 200;
        } catch (Exception e) { return false; }
    }

    // --- HÀM TÌM KIẾM ---
    public static DirectoryContentResponse searchFiles(String keyword) {
        try {
            String encodedKeyword = java.net.URLEncoder.encode(keyword, java.nio.charset.StandardCharsets.UTF_8);
            String url = "/api/files/search?query=" + encodedKeyword;
            HttpRequest request = createRequestBuilder(url).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return gson.fromJson(response.body(), DirectoryContentResponse.class);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    // --- PROFILE ---
    public static class UserProfileDto {
        public Long id;
        public String email;
        public String username;
        public String dateOfBirth;
        public String nationality;
    }

    public static UserProfileDto getUserProfile() {
        try {
            HttpRequest request = createRequestBuilder("/api/account/profile").GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return gson.fromJson(response.body(), UserProfileDto.class);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }
}