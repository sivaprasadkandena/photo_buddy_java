package com.photobuddy.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "cloudinary")
public class CloudinaryFileStorageService implements FileStorageService {
    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;
    private final RestClient client = RestClient.create();
    public CloudinaryFileStorageService(@Value("${app.storage.cloudinary.cloud-name}") String cloudName,
                                        @Value("${app.storage.cloudinary.api-key}") String apiKey,
                                        @Value("${app.storage.cloudinary.api-secret}") String apiSecret) {
        this.cloudName = cloudName; this.apiKey = apiKey; this.apiSecret = apiSecret;
    }
    @Override public String storeImage(MultipartFile file) {
        ImageFileValidator.extension(file);
        long timestamp = System.currentTimeMillis() / 1000;
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", new ByteArrayResource(bytes(file)) {
            @Override public String getFilename() { return "photo-buddy-upload"; }
        });
        parts.add("api_key", apiKey); parts.add("timestamp", Long.toString(timestamp));
        parts.add("signature", signature("timestamp=" + timestamp));
        Map<?, ?> response = client.post().uri(endpoint("image/upload")).contentType(MediaType.MULTIPART_FORM_DATA)
                .body(parts).retrieve().body(Map.class);
        if (response == null || response.get("secure_url") == null) throw new IllegalStateException("Cloud image upload failed");
        return response.get("secure_url").toString();
    }
    @Override public void delete(String url) {
        String publicId = publicId(url);
        if (publicId == null) return;
        long timestamp = System.currentTimeMillis() / 1000;
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("public_id", publicId); parts.add("api_key", apiKey); parts.add("timestamp", Long.toString(timestamp));
        parts.add("signature", signature("public_id=" + publicId + "&timestamp=" + timestamp));
        client.post().uri(endpoint("image/destroy")).contentType(MediaType.MULTIPART_FORM_DATA).body(parts).retrieve().toBodilessEntity();
    }
    private String endpoint(String action) { return "https://api.cloudinary.com/v1_1/" + cloudName + "/" + action; }
    private String signature(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest((value + apiSecret).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
    private byte[] bytes(MultipartFile file) {
        try { return file.getBytes(); } catch (IOException exception) { throw new IllegalArgumentException("Image could not be read", exception); }
    }
    private String publicId(String url) {
        if (url == null || !url.contains("res.cloudinary.com/") || !url.contains("/image/upload/")) return null;
        String path = url.substring(url.indexOf("/image/upload/") + "/image/upload/".length());
        path = path.replaceFirst("^v[0-9]+/", "");
        int dot = path.lastIndexOf('.');
        return dot < 0 ? path : path.substring(0, dot);
    }
}
