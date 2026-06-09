package com.example.lab3392.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FileStorageService {

    private final Path uploadDir;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png");
    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024;

    public FileStorageService(@Value("${app.upload.dir:/app/uploads}") String uploadDirPath) {
        this.uploadDir = Paths.get(uploadDirPath);
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("无法创建上传目录: " + uploadDirPath, e);
        }
    }

    public String store(InputStream inputStream, String originalFilename, String contentType) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        String extension = getExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("仅支持 JPG/PNG 格式的图片");
        }

        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("仅支持 JPG/PNG 格式的图片");
        }

        String filename = UUID.randomUUID().toString() + extension;
        Path target = uploadDir.resolve(filename);

        try {
            long size = Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            if (size > MAX_FILE_SIZE) {
                Files.deleteIfExists(target);
                throw new IllegalArgumentException("图片大小不能超过 2MB");
            }
        } catch (IOException e) {
            throw new RuntimeException("文件保存失败", e);
        }

        return filename;
    }

    public void delete(String filename) {
        if (filename == null || filename.isBlank()) return;
        try {
            Files.deleteIfExists(uploadDir.resolve(filename));
        } catch (IOException ignored) {
        }
    }

    public Path resolve(String filename) {
        return uploadDir.resolve(filename);
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0) return "";
        return filename.substring(dotIndex);
    }
}
