package com.planner.file;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Map<String, String> ALLOWED = Map.of(
        "image/png", ".png",
        "image/jpeg", ".jpg");

    private final Path avatarDir;

    public FileStorageService(@Value("${app.storage.dir}") String storageDir) {
        this.avatarDir = Paths.get(storageDir, "avatars");
        try {
            Files.createDirectories(this.avatarDir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String storeAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        String extension = ALLOWED.get(file.getContentType());
        if (extension == null) {
            throw new IllegalArgumentException("Only PNG and JPEG images are allowed");
        }
        String filename = UUID.randomUUID() + extension;
        try {
            Files.copy(file.getInputStream(), avatarDir.resolve(filename),
                StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return filename;
    }

    public Resource loadAvatar(String filename) {
        try {
            Path file = avatarDir.resolve(filename).normalize();
            if (!file.startsWith(avatarDir)) {
                throw new IllegalArgumentException("Invalid path");
            }
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists()) {
                throw new java.util.NoSuchElementException("File not found");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid path", e);
        }
    }
}
