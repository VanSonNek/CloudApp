package com.example.demo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;

import com.google.gson.Gson;


// Lớp hỗ trợ cho request tạo thư mục
class CreateDirectoryRequest {
    public String name;
    public Long parentDirectoryId;

    public CreateDirectoryRequest(String name, Long parentDirectoryId) {
        this.name = name;
        this.parentDirectoryId = parentDirectoryId;
    }
}

// Lớp hỗ trợ cho Response khi tạo thư mục
class CreateDirectoryResponse {
    public Long id;
    public String name;
    public Long parentDirectoryId;
    public Long userId;
    // Thêm các trường khác nếu server trả về
}

// ✅ THÊM LỚP NÀY: Ánh xạ Response từ Server
class DirectoryContentResponse {
    public List<ListItem.DirectoryDto> directories;
    public List<ListItem.FileDto> files;
}

// Lớp hỗ trợ cho Login (cần thiết nếu có)
class LoginRequest {
    public String username;
    public String password;

    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }
}


// Handler chứa logic mạng và trạng thái đăng nhập
public class ClientApiHandler {

    private static final String SERVER_URL = "http://localhost:8080";
    private static final Gson gson = new Gson();
    private static final HttpClient client = HttpClient.newBuilder().build();

    private static String username;
    private static String password;
    private static boolean authenticated = false;

    public static boolean isAuthenticated() {
        return authenticated;
    }

    // ✅ SỬA LỖI: Implement phương thức createRequestBuilder
    private static HttpRequest.Builder createRequestBuilder(String url) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(SERVER_URL + url))
                .header("Content-Type", "application/json");

        if (authenticated) {
            String auth = username + ":" + password;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            builder.header("Authorization", "Basic " + encodedAuth);
        }
        return builder;
    }

    public static boolean login(String u, String p) {
        try {
            LoginRequest request = new LoginRequest(u, p);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(SERVER_URL + "/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(request)))
                    .build();

            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                username = u;
                password = p;
                authenticated = true;
                return true;
            } else {
                authenticated = false;
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            authenticated = false;
            return false;
        }
    }

    public static void createFolder(String name, Long parentDirectoryId) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                CreateDirectoryRequest request = new CreateDirectoryRequest(name, parentDirectoryId);
                HttpRequest httpRequest = createRequestBuilder("/api/directories")
                        .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(request)))
                        .build();

                HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 201) {
                    System.out.println("Tạo thư mục thành công: " + response.body());
                } else {
                    System.err.println("Tạo thư mục thất bại. Status: " + response.statusCode());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // ✅ SỬA LỖI LOGIC MẠNG: Hàm chung để gọi API content
    private static DirectoryContentResponse getDirectoryContent(Long directoryId) throws Exception {
        String url = "/api/directories/content";
        if (directoryId != null) {
            // Thêm tham số directoryId vào URL
            url += "?directoryId=" + directoryId;
        }

        HttpRequest httpRequest = createRequestBuilder(url)
                .GET()
                .build();

        HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return gson.fromJson(response.body(), DirectoryContentResponse.class);
        } else {
            System.err.println("Lỗi tải nội dung thư mục: " + response.body());
            throw new IOException("Lỗi Server: " + response.statusCode());
        }
    }

    // ✅ SỬA LỖI LOGIC MẠNG: getDirectories gọi hàm chung
    public static List<ListItem.DirectoryDto> getDirectories(Long parentId) throws Exception {
        DirectoryContentResponse response = getDirectoryContent(parentId);
        return response != null ? response.directories : Collections.emptyList();
    }

    // ✅ SỬA LỖI LOGIC MẠNG: getFiles gọi hàm chung
    public static List<ListItem.FileDto> getFiles(Long directoryId) throws Exception {
        DirectoryContentResponse response = getDirectoryContent(directoryId);
        return response != null ? response.files : Collections.emptyList();
    }

    // ================= UPLOAD FILE (Giữ nguyên logic Multipart) =================
    private static final String BOUNDARY = "----WebKitFormBoundary" + System.currentTimeMillis();

    public static void uploadFile(File file, Long directoryId) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String url = "/api/files/upload";
                if (directoryId != null) {
                    url += "?directoryId=" + directoryId;
                }

                HttpRequest httpRequest = createRequestBuilder(url)
                        .header("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
                        .POST(new MultipartBodyPublisher(file, directoryId))
                        .build();

                HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    System.out.println("Tải file thành công: " + response.body());
                } else {
                    System.err.println("Tải file thất bại. Status: " + response.statusCode());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void uploadDirectory(File dir, Long currentDirectoryId) {
        // Cần thêm logic upload thư mục
    }

    // Lớp nội bộ để hỗ trợ upload multipart/form-data
    static class MultipartBodyPublisher implements HttpRequest.BodyPublisher {
        private final File file;
        private final Long directoryId;
        private final byte[] header;
        private final byte[] footer;
        private final long totalLength;

        public MultipartBodyPublisher(File file, Long directoryId) throws IOException {
            this.file = file;
            this.directoryId = directoryId;

            String boundary = BOUNDARY;

            // Xây dựng Header cho file
            String filePart = String.format("--%s\r\nContent-Disposition: form-data; name=\"file\"; filename=\"%s\"\r\nContent-Type: %s\r\n\r\n",
                    boundary, file.getName(), Files.probeContentType(file.toPath()));
            this.header = filePart.getBytes();

            // Xây dựng Footer
            this.footer = ("\r\n--" + boundary + "--\r\n").getBytes();

            this.totalLength = header.length + file.length() + footer.length;
        }

        @Override
        public long contentLength() {
            return totalLength;
        }

        @Override
        public void subscribe(java.util.concurrent.Flow.Subscriber<? super java.nio.ByteBuffer> subscriber) {
            try {
                subscriber.onSubscribe(new FileSubscription(subscriber, file, header, footer));
            } catch (FileNotFoundException e) {
                subscriber.onError(e);
            }
        }

        // ... (FileSubscription class)
        static class FileSubscription implements Flow.Subscription {
            private final Flow.Subscriber<? super ByteBuffer> subscriber;
            private final File file;
            private final byte[] header;
            private final byte[] footer;
            private FileInputStream fileStream;
            private int state = 0; // 0: header, 1: file, 2: footer, 3: finished
            private int headerPos = 0;
            private int footerPos = 0;
            private long currentPosition = 0;

            public FileSubscription(Flow.Subscriber<? super ByteBuffer> subscriber, File file, byte[] header, byte[] footer) throws FileNotFoundException {
                this.subscriber = subscriber;
                this.file = file;
                this.header = header;
                this.footer = footer;
                this.fileStream = new FileInputStream(file);
            }

            @Override
            public void request(long n) {
                if (n <= 0) {
                    subscriber.onError(new IllegalArgumentException("request must be positive"));
                    return;
                }
                
                try {
                    byte[] buffer = new byte[(int) Math.min(n, 8192)]; 
                    int bytesRead = 0;
                    int len = buffer.length;
                    int off = 0;
                    
                    while (len > 0 && state < 3) {
                        int currentRead = 0;
                        
                        if (state == 0) { // Đọc Header
                            int remaining = header.length - headerPos;
                            currentRead = Math.min(len, remaining);
                            System.arraycopy(header, headerPos, buffer, off, currentRead);
                            headerPos += currentRead;
                            if (headerPos == header.length) state = 1;
                        } else if (state == 1) { // Đọc File
                            currentRead = fileStream.read(buffer, off, len);
                            if (currentRead == -1) {
                                state = 2;
                                fileStream.close();
                                continue; 
                            }
                        } else if (state == 2) { // Đọc Footer
                            int remaining = footer.length - footerPos;
                            currentRead = Math.min(len, remaining);
                            System.arraycopy(footer, footerPos, buffer, off, currentRead);
                            footerPos += currentRead;
                            if (footerPos == footer.length) state = 3;
                        }
                        
                        if (currentRead > 0) {
                            bytesRead += currentRead;
                            off += currentRead;
                            len -= currentRead;
                            currentPosition += currentRead;
                        } else if (state == 3) {
                            break; 
                        } else if (currentRead == 0) {
                            break; 
                        }
                    }

                    if (bytesRead > 0) {
                        subscriber.onNext(ByteBuffer.wrap(buffer, 0, bytesRead));
                    }
                    
                    if (state == 3) {
                        subscriber.onComplete();
                    }
                    
                } catch (IOException e) {
                    subscriber.onError(e);
                }
            }

            @Override
            public void cancel() {
                try {
                    if (fileStream != null) fileStream.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }
}