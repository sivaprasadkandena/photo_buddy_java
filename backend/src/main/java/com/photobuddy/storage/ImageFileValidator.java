package com.photobuddy.storage;

import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

final class ImageFileValidator {
    private ImageFileValidator() {}

    static String extension(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > 10L * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose an image up to 10 MB");
        }
        try {
            byte[] header = file.getBytes();
            String type = file.getContentType();
            if (type != null && type.equalsIgnoreCase("image/jpeg") && header.length >= 3
                    && (header[0] & 255) == 255 && (header[1] & 255) == 216 && (header[2] & 255) == 255) return ".jpg";
            if (type != null && type.equalsIgnoreCase("image/png") && header.length >= 8
                    && (header[0] & 255) == 137 && header[1] == 80 && header[2] == 78 && header[3] == 71) return ".png";
            if (type != null && type.equalsIgnoreCase("image/webp") && header.length >= 12
                    && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                    && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') return ".webp";
        } catch (IOException ignored) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image could not be read");
        }
        throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Use a valid JPEG, PNG, or WebP image");
    }
}
