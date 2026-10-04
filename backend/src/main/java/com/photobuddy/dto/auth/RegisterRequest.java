package com.photobuddy.dto.auth;

import com.photobuddy.entity.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 60) String firstName,
        @NotBlank @Size(max = 60) String lastName,
        @NotBlank @Size(min = 3, max = 30)
        @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "Username may contain letters, numbers, and underscores")
        String username,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{12,72}$",
                message = "Password must be 12–72 characters with lowercase, uppercase, number, and symbol")
        String password,
        @NotBlank String confirmPassword,
        @NotNull Gender gender,
        @Size(max = 500) String bio,
        boolean isPhotographer,
        @Size(max = 2048) @Pattern(regexp = "^$|^https?://.+$", message = "Profile picture must be an HTTP(S) URL")
        String profilePicture) {
}
