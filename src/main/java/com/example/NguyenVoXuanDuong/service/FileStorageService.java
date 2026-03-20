package com.example.NguyenVoXuanDuong.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {
    private static final Path STATIC_ROOT = Paths.get("src", "main", "resources", "static");

    public String storeImage(MultipartFile file, String relativeDirectory) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String originalFilename = file.getOriginalFilename();
        String extension = extractExtension(originalFilename);
        String filename = UUID.randomUUID() + extension;
        String normalizedDirectory = normalizeDirectory(relativeDirectory);
        Path targetDirectory = STATIC_ROOT.resolve(normalizedDirectory);
        Path targetFile = targetDirectory.resolve(filename);

        try {
            Files.createDirectories(targetDirectory);
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
            return "/" + normalizedDirectory.replace('\\', '/') + "/" + filename;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store file: " + originalFilename, ex);
        }
    }

    private String extractExtension(String filename) {
        if (filename == null) {
            return "";
        }

        String sanitized = Paths.get(filename).getFileName().toString();
        int extensionIndex = sanitized.lastIndexOf('.');
        return extensionIndex >= 0 ? sanitized.substring(extensionIndex) : "";
    }

    private String normalizeDirectory(String relativeDirectory) {
        return relativeDirectory == null ? "images" : relativeDirectory.replace('/', '\\').replaceAll("^\\\\+|\\\\+$", "");
    }
}
