package com.shofe.service;

import com.shofe.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path root;

    public FileStorageService(@Value("${shofe.uploads-dir}") String uploadsDir) throws IOException {
        this.root = Path.of(uploadsDir).toAbsolutePath().normalize();
        Files.createDirectories(this.root);
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0 && dot < original.length() - 1) {
            ext = original.substring(dot).toLowerCase();
            if (!ext.matches("\\.(jpg|jpeg|png|pdf|webp)")) {
                throw ApiException.badRequest("Documents must be JPG, PNG, WEBP, or PDF");
            }
        }
        String name = UUID.randomUUID() + ext;
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, root.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw ApiException.badRequest("Could not store the uploaded file");
        }
        return "/uploads/" + name;
    }
}
