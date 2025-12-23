package com.example.demo;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

// Lớp này phải là public
public class ListItem {
    private Long id;
    private String name;
    private String type; // "Folder" hoặc "File"
    private Long sizeBytes;
    private LocalDateTime dateCreated;

    // --- DTOs (lớp con tĩnh) để Gson parse JSON từ Server ---
    public static class DirectoryDto {
        public Long id;
        public String name;
        public String createdAt; 
        public Long parentDirectoryId;
        public Long userId;
    }

    public static class FileDto {
        public Long id;
        public String originalFilename;
        public String storedFilename;
        public String type; // Ví dụ: "image/png", "application/pdf"
        public Long size;   // Kích thước tính bằng byte
        public String uploadDate;
        public Long folderId;
        public String ownerName; // Tên người sở hữu (để hiện ở cột Owner)

        // Trạng thái
        public boolean inTrash;   // Đang ở thùng rác hay không
        public boolean isStarred; // Đã yêu thích hay chưa (New)

        // Constructor mặc định (cần thiết cho thư viện Gson/Jackson)
        public FileDto() {}

        // Constructor tiện ích (nếu cần test nhanh)
        public FileDto(Long id, String originalFilename, Long size, boolean isStarred) {
            this.id = id;
            this.originalFilename = originalFilename;
            this.size = size;
            this.isStarred = isStarred;
        }

        @Override
        public String toString() {
            return originalFilename; // Để hiển thị tên khi debug hoặc cho vào ListView đơn giản
        }
    }
    // ------------------------------------------
    
    // Constructor cho Thư mục
    public ListItem(DirectoryDto dirDto) {
        this.id = dirDto.id;
        this.name = dirDto.name;
        this.type = "Folder";
        this.sizeBytes = 0L;
        this.dateCreated = parseDate(dirDto.createdAt);
    }

    // Constructor cho File
    public ListItem(FileDto fileDto) {
        this.id = fileDto.id;
        this.name = fileDto.originalFilename;
        this.type = getFileType(fileDto.originalFilename);
        this.sizeBytes = fileDto.size;
        this.dateCreated = parseDate(fileDto.uploadDate);
    }
    
    private LocalDateTime parseDate(String dateString) {
        if (dateString == null) return null;
        try {
            return LocalDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException e) {
            System.err.println("Lỗi parse ngày: " + dateString);
            return null;
        }
    }
    
    private String getFileType(String filename) {
        if (filename == null || filename.isEmpty() || !filename.contains(".")) {
            return "File";
        }
        String extension = filename.substring(filename.lastIndexOf(".") + 1);
        return extension.toUpperCase() + " File";
    }

    // ===================================
    // Getters cho TableView (BẮT BUỘC)
    // ===================================

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public Long getSizeBytes() { return sizeBytes; }
    
    public String getSize() {
        if (type.equals("Folder")) {
            return ""; 
        }
        return formatSize(sizeBytes);
    }
    
    public String getDate() {
        if (dateCreated != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            return dateCreated.format(formatter);
        }
        return "";
    }
    
    private String formatSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}