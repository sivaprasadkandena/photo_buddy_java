package com.photobuddy.controller;

import com.photobuddy.storage.LocalFileStorageService;
import java.nio.file.Path;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalFileController {
    private final LocalFileStorageService storage;
    public LocalFileController(LocalFileStorageService storage) { this.storage = storage; }
    @GetMapping("/api/files/{key}")
    public ResponseEntity<Resource> image(@PathVariable String key) {
        Path path = storage.getFile(key);
        String name = path.getFileName().toString();
        MediaType media = name.endsWith(".png") ? MediaType.IMAGE_PNG : name.endsWith(".webp")
                ? MediaType.parseMediaType("image/webp") : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(media).cacheControl(CacheControl.noCache()).body(new FileSystemResource(path));
    }
}
