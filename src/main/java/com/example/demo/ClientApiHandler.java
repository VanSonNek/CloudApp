package com.example.demo;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

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
// Quan trọng: Phải khớp với JSON trả về từ Server, cần có ID
class CreateDirectoryResponse {
    public Long id;
    public String name;
    public Long parentDirectoryId;
    public Long userId;
    // ... (các trường khác nếu Server trả về)
}

// Handler chứa logic mạng và trạng thái đăng nhập
public class ClientApiHandler {

    private static final String SERVER_URL = "http://localhost:8080";
    private static final Gson gson = new Gson();
    private static final HttpClient client = HttpClient.newHttpClient();
    
    // THÔNG TIN AUTHENTICATION (HTTP BASIC)
    private static String currentUsername; 
    private static String currentPassword; 
    
    // --- LƯU TRỮ/CẬP NHẬT THÔNG TIN ĐĂNG NHẬP ---
    public static void setCredentials(String username, String password) {
        currentUsername = username;
        currentPassword = password;
    }

    // ================= LẤY DANH SÁCH THƯ MỤC =================
    public static List<ListItem.DirectoryDto> getDirectories(Long parentId) {
        String authHeader = getBasicAuthHeader();
        if (authHeader == null) {
            System.err.println("Chưa đăng nhập!");
            return Collections.emptyList();
        }

        try {
            String url = SERVER_URL + "/api/directories";
            if (parentId != null) {
                url += "?parentId=" + parentId;
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", authHeader)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                ListItem.DirectoryDto[] array = gson.fromJson(response.body(), ListItem.DirectoryDto[].class);
                return Arrays.asList(array);
            } else {
                System.err.println("Lỗi lấy thư mục: " + response.statusCode() + " - " + response.body());
                return Collections.emptyList();
            }
        } catch (Exception e) {
            System.err.println("Lỗi kết nối khi lấy danh sách thư mục");
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    // ================= LẤY DANH SÁCH FILE =================
    public static List<ListItem.FileDto> getFiles(Long directoryId) {
        String authHeader = getBasicAuthHeader();
        if (authHeader == null) {
            System.err.println("Chưa đăng nhập!");
            return Collections.emptyList();
        }

        try {
            String url = SERVER_URL + "/api/files";
            if (directoryId != null) {
                url += "?directoryId=" + directoryId;
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", authHeader)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                ListItem.FileDto[] array = gson.fromJson(response.body(), ListItem.FileDto[].class);
                return Arrays.asList(array);
            } else {
                System.err.println("Lỗi lấy file: " + response.statusCode() + " - " + response.body());
                return Collections.emptyList();
            }
        } catch (Exception e) {
            System.err.println("Lỗi kết nối khi lấy danh sách file");
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
    
    public static boolean isAuthenticated() {
        return currentUsername != null && currentPassword != null;
    }

    private static String getBasicAuthHeader() {
        if (!isAuthenticated()) return null;
        String credentials = currentUsername + ":" + currentPassword;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes()); 
    }

    // --- CẬP NHẬT: UPLOAD FILE (Hỗ trợ Directory ID) ---
    public static void uploadFile(File file, Long directoryId) {
        String authHeader = getBasicAuthHeader();
        if (authHeader == null) return;
        
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // SỬA: Thêm tham số directoryId vào URL
                String url = SERVER_URL + "/api/files/upload";
                if (directoryId != null) {
                    url += "?directoryId=" + directoryId;
                }
                
                String boundary = "---boundary-java-client---";
                
                // Content-Type cho multipart/form-data
                HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.ofInputStream(() -> {
                    try {
                        // Tạo body multipart (tương tự logic cũ, nhưng dùng OutputStream)
                        return new MultipartInputStream(file, boundary);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

                HttpRequest uploadRequest = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Authorization", authHeader)
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(bodyPublisher)
                        .build();

                HttpResponse<String> response = client.send(uploadRequest, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    System.out.println("✅ Tải file thành công: " + file.getName());
                } else if (response.statusCode() == 403) {
                    System.err.println("❌ LỖI TẢI FILE: Status 403 (Forbidden - Thiếu quyền).");
                } else {
                    System.err.println("❌ LỖI TẢI FILE: Status " + response.statusCode() + ". Phản hồi: " + response.body());
                }
            } catch (Exception e) {
                System.err.println("Lỗi khi tải lên file " + file.getName());
                e.printStackTrace();
            }
        });
    }

    // --- THÊM: UPLOAD THƯ MỤC (Khởi động) ---
    public static void uploadDirectory(File rootDirectory, Long parentDirectoryId) {
        String authHeader = getBasicAuthHeader();
        if (authHeader == null) return;
        
        // Chạy quá trình duyệt và upload trên luồng riêng
        Executors.newSingleThreadExecutor().execute(() -> {
            System.out.println("\n*** Bắt đầu tải lên thư mục: " + rootDirectory.getName() + " ***");
            // Bắt đầu upload đệ quy
            uploadDirectoryRecursive(rootDirectory, parentDirectoryId);
            System.out.println("*** Hoàn thành tải lên thư mục ***\n");
        });
    }
    
    // --- THÊM: HÀM TẢI LÊN ĐỆ QUY (QUAN TRỌNG) ---
    private static void uploadDirectoryRecursive(File currentFileOrDirectory, Long parentDirectoryId) {
        if (!currentFileOrDirectory.exists()) return;

        if (currentFileOrDirectory.isDirectory()) {
            
            // 1. TẠO THƯ MỤC TRÊN SERVER
            String dirName = currentFileOrDirectory.getName();
            
            // Tạm thời tạo thư mục trên Server (để lấy ID)
            Long newDirectoryId = createFolderAndGetId(dirName, parentDirectoryId);
            
            if (newDirectoryId != null) {
                System.out.println("-> [OK] Đã tạo thư mục: " + dirName + " (ID: " + newDirectoryId + ")");
                
                // 2. DUYỆT CÁC PHẦN TỬ CON
                File[] children = currentFileOrDirectory.listFiles();
                if (children != null) {
                    for (File child : children) {
                        // Gọi đệ quy cho các thư mục con và file con, dùng ID mới làm thư mục cha
                        uploadDirectoryRecursive(child, newDirectoryId);
                    }
                }
            } else {
                System.err.println("!!! [LỖI] Không thể tạo thư mục " + dirName + ", bỏ qua nội dung bên trong.");
            }
        } else if (currentFileOrDirectory.isFile()) {
            // 3. TẢI FILE LÊN SERVER
            System.out.println("   - Đang tải file: " + currentFileOrDirectory.getName());
            
            // Gọi hàm uploadFile đã cập nhật, dùng parentDirectoryId (ID thư mục cha)
            uploadFile(currentFileOrDirectory, parentDirectoryId);
            
            // Thêm một chút delay để tránh quá tải Server
            try {
                Thread.sleep(100); 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // --- CẬP NHẬT: TẠO THƯ MỤC (Trả về ID) ---
    // Hàm này được gọi từ AllfileController (dùng in ra console) và uploadDirectoryRecursive (dùng lấy ID)
    public static Long createFolder(String folderName, Long parentDirectoryId) {
        return createFolderAndGetId(folderName, parentDirectoryId, true);
    }
    
    // Hàm nội bộ tạo thư mục và trả về ID (có thể tắt log)
    private static Long createFolderAndGetId(String folderName, Long parentDirectoryId) {
        return createFolderAndGetId(folderName, parentDirectoryId, false);
    }

    private static Long createFolderAndGetId(String folderName, Long parentDirectoryId, boolean shouldLog) {
        String authHeader = getBasicAuthHeader();
        if (authHeader == null) return null;
        
        try {
            String jsonBody = gson.toJson(new CreateDirectoryRequest(folderName, parentDirectoryId));

            HttpRequest createRequest = HttpRequest.newBuilder()
                    .uri(URI.create(SERVER_URL + "/api/directories"))
                    .header("Authorization", authHeader)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(createRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201) {
                if(shouldLog) {
                    System.out.println("✅ Tạo thư mục thành công: " + folderName);
                }
                // CHUYỂN JSON THÀNH OBJECT ĐỂ LẤY ID
                CreateDirectoryResponse dirResponse = gson.fromJson(response.body(), CreateDirectoryResponse.class);
                return dirResponse.id;
            } else if (response.statusCode() == 403) {
                System.err.println("❌ Lỗi Server khi t?o thư m?c: 403 (Forbidden - Thiếu quyền). Đảm bảo Server SecurityConfig đã sửa.");
            } else {
                System.err.println("❌ Lỗi Server khi tạo thư mục: " + response.statusCode() + ". Phản hồi: " + response.body());
            }
        } catch (Exception e) {
            System.err.println("Lỗi kết nối khi tạo thư mục.");
            e.printStackTrace();
        }
        return null;
    }

    // --- CÁC HÀM CŨ KHÁC ---
    public static void fetchDataAndPrint() {
        // ... (giữ nguyên logic cũ)
    }
}


/**
 * Lớp hỗ trợ tạo Body Publisher cho multipart/form-data
 * Đây là cách xử lý cần thiết để Java HttpClient gửi file.
 */
class MultipartInputStream extends java.io.InputStream {
    private final File file;
    private final String boundary;
    private final byte[] header;
    private final byte[] footer;
    private final java.io.FileInputStream fileStream;
    private long totalLength;
    private long currentPosition = 0;
    
    // Trạng thái: 0=header, 1=file, 2=footer, 3=end
    private int state = 0; 
    private int headerPos = 0;
    private int footerPos = 0;

    public MultipartInputStream(File file, String boundary) throws IOException {
        this.file = file;
        this.boundary = boundary;

        // Tạo Header: Phần này mô tả form field 'file' và tên file
        String headerStr = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n" +
                "Content-Type: " + Files.probeContentType(file.toPath()) + "\r\n\r\n";
        this.header = headerStr.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        // Tạo Footer: Phần này kết thúc body
        String footerStr = "\r\n--" + boundary + "--\r\n";
        this.footer = footerStr.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        this.fileStream = new java.io.FileInputStream(file);
        this.totalLength = header.length + file.length() + footer.length;
    }

    @Override
    public int read() throws IOException {
        // Đọc từng byte một từ các phần (header, file, footer)
        if (state == 0) {
            if (headerPos < header.length) {
                currentPosition++;
                return header[headerPos++];
            }
            state = 1;
        }

        if (state == 1) {
            int fileByte = fileStream.read();
            if (fileByte != -1) {
                currentPosition++;
                return fileByte;
            }
            state = 2;
            fileStream.close(); // Đóng stream file sau khi đọc xong
        }

        if (state == 2) {
            if (footerPos < footer.length) {
                currentPosition++;
                return footer[footerPos++];
            }
            state = 3;
        }

        return -1; // End of stream
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        // Đọc một khối dữ liệu để tối ưu hiệu suất (quan trọng)
        int bytesRead = 0;
        
        while (len > 0 && state < 3) {
            int currentRead = 0;
            
            if (state == 0) { // Đọc Header
                int remaining = header.length - headerPos;
                currentRead = Math.min(len, remaining);
                System.arraycopy(header, headerPos, b, off, currentRead);
                headerPos += currentRead;
                if (headerPos == header.length) state = 1;
            } else if (state == 1) { // Đọc File
                currentRead = fileStream.read(b, off, len);
                if (currentRead == -1) {
                    state = 2;
                    fileStream.close();
                    continue; // Quay lại vòng lặp để đọc Footer
                }
            } else if (state == 2) { // Đọc Footer
                int remaining = footer.length - footerPos;
                currentRead = Math.min(len, remaining);
                System.arraycopy(footer, footerPos, b, off, currentRead);
                footerPos += currentRead;
                if (footerPos == footer.length) state = 3;
            }
            
            if (currentRead > 0) {
                bytesRead += currentRead;
                off += currentRead;
                len -= currentRead;
                currentPosition += currentRead;
            } else if (state == 3) {
                break; // Kết thúc
            } else if (currentRead == 0) {
                break; // Dừng lại nếu không đọc được gì
            }
        }
        
        return bytesRead > 0 ? bytesRead : -1;
    }
}