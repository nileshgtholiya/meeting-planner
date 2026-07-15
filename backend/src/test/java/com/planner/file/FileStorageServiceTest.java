package com.planner.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    @Test
    void storesValidImageAndReturnsFilename(@TempDir Path dir) {
        FileStorageService service = new FileStorageService(dir.toString());
        MockMultipartFile file = new MockMultipartFile(
            "file", "a.png", "image/png", new byte[]{1, 2, 3});
        String name = service.storeAvatar(file);
        assertNotNull(name);
        assertTrue(name.endsWith(".png"));
    }

    @Test
    void rejectsNonImage(@TempDir Path dir) {
        FileStorageService service = new FileStorageService(dir.toString());
        MockMultipartFile file = new MockMultipartFile(
            "file", "a.txt", "text/plain", new byte[]{1});
        assertThrows(IllegalArgumentException.class, () -> service.storeAvatar(file));
    }

    @Test
    void rejectsEmptyFile(@TempDir Path dir) {
        FileStorageService service = new FileStorageService(dir.toString());
        MockMultipartFile file = new MockMultipartFile(
            "file", "a.png", "image/png", new byte[]{});
        assertThrows(IllegalArgumentException.class, () -> service.storeAvatar(file));
    }
}
