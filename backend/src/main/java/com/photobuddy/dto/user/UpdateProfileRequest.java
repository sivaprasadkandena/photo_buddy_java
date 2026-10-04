package com.photobuddy.dto.user;

import com.photobuddy.entity.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 60) String firstName,
        @NotBlank @Size(max = 60) String lastName,
        @Size(max = 500) String bio,
        @NotNull Gender gender,
        boolean isPhotographer,
        @Size(max = 2048)
        @Pattern(regexp = "^$|^https?://[^\\s]+$", message = "Profile picture must be an HTTP(S) URL")
        String profilePicture) {}
