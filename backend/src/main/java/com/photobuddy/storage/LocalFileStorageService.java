package com.photobuddy.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {
    private final Path root;
    private final String publicBaseUrl;
    public LocalFileStorageService(@Value("${app.storage.local-directory:./uploads}") String directory,
                                   @Value("${app.storage.public-base-url:http://localhost:8080}") String publicBaseUrl) {
        root = Path.of(directory).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl.replaceAll("/$", "");
    }
    @Override public String storeImage(MultipartFile file) {
        String key = UUID.randomUUID() + ImageFileValidator.extension(file);
        try {
            Files.createDirectories(root);
            Files.copy(file.getInputStream(), root.resolve(key));
            return publicBaseUrl + "/api/files/" + key;
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Image could not be saved");
        }
    }
    @Override public void delete(String url) {
        if (url == null || !url.contains("/api/files/")) return;
        String key = url.substring(url.lastIndexOf("/api/files/") + "/api/files/".length());
        if (!key.matches("[a-f0-9-]+\\.(jpg|png|webp)")) return;
        try { Files.deleteIfExists(root.resolve(key).normalize()); }
        catch (IOException ignored) { }
    }
    public Path getFile(String key) {
        if (key == null || !key.matches("[a-f0-9-]+\\.(jpg|png|webp)")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image was not found");
        }
        Path file = root.resolve(key).normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image was not found");
        }
        return file;
    }
}
