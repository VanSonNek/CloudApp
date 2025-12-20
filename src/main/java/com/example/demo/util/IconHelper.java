package com.example.demo.util;

import java.io.InputStream;

import javafx.scene.image.Image;

public class IconHelper {

    // Đường dẫn gốc chứa ảnh
    private static final String IMG_PATH = "/com/example/demo/imgs/";

    /**
     * Hàm lấy Image dựa trên loại và tên file
     * @param itemType "FILE" hoặc "FOLDER"
     * @param fileName Tên file (ví dụ: baocao.docx)
     * @return Đối tượng Image JavaFX
     */
    public static Image getFileIcon(String itemType, String fileName) {
        String iconPath = getIconPathString(itemType, fileName);
        
        try {
            // Cố gắng tải ảnh đúng
            InputStream is = IconHelper.class.getResourceAsStream(iconPath);
            if (is != null) {
                return new Image(is);
            }
        } catch (Exception e) {
            // Bỏ qua lỗi
        }

        // Nếu lỗi hoặc không tìm thấy ảnh, trả về ảnh mặc định (file.png)
        try {
            return new Image(IconHelper.class.getResourceAsStream(IMG_PATH + "file.png"));
        } catch (Exception e) {
            return null; // Trường hợp xấu nhất (mất cả file.png)
        }
    }

    // Logic phân loại đuôi file (Tách riêng để dễ quản lý)
    private static String getIconPathString(String itemType, String fileName) {
        if ("FOLDER".equals(itemType)) {
            return IMG_PATH + "folder.png";
        }

        if (fileName == null) return IMG_PATH + "file.png";
        String lower = fileName.toLowerCase();

        // 1. OFFICE
        if (lower.endsWith(".doc") || lower.endsWith(".docx")) return IMG_PATH + "word.png";
        if (lower.endsWith(".xls") || lower.endsWith(".xlsx") || lower.endsWith(".csv")) return IMG_PATH + "excel.png";
        if (lower.endsWith(".ppt") || lower.endsWith(".pptx")) return IMG_PATH + "ppt.png";

        // 2. PDF & TEXT
        if (lower.endsWith(".pdf")) return IMG_PATH + "pdf.png";
        if (lower.endsWith(".txt") || lower.endsWith(".log") || lower.endsWith(".md")) return IMG_PATH + "txt.png";

        // 3. CODE
        if (lower.endsWith(".java") || lower.endsWith(".html") || lower.endsWith(".css") || 
            lower.endsWith(".js") || lower.endsWith(".json") || lower.endsWith(".xml") || lower.endsWith(".sql")) {
            return IMG_PATH + "code.png";
        }

        // 4. FILE NÉN
        if (lower.endsWith(".zip") || lower.endsWith(".rar") || lower.endsWith(".7z") || lower.endsWith(".tar")) {
            return IMG_PATH + "zip.png";
        }

        // 5. MEDIA (ẢNH, NHẠC, VIDEO)
        if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".gif") || lower.endsWith(".bmp")) {
            return IMG_PATH + "image.png";
        }
        if (lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".flac")) {
            return IMG_PATH + "music.png";
        }
        if (lower.endsWith(".mp4") || lower.endsWith(".avi") || lower.endsWith(".mkv") || lower.endsWith(".mov")) {
            return IMG_PATH + "video.png";
        }

        // Mặc định
        return IMG_PATH + "file.png";
    }
}