package com.photobuddy.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String storeImage(MultipartFile file);
    void delete(String url);
}
