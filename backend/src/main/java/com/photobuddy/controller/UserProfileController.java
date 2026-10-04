package com.photobuddy.controller;

import com.photobuddy.dto.user.ProfileResponse;
import com.photobuddy.dto.user.UpdateProfileRequest;
import com.photobuddy.security.AuthenticatedUser;
import com.photobuddy.service.impl.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/users", "/api/v1/users"})
public class UserProfileController {
    private final UserProfileService profiles;

    public UserProfileController(UserProfileService profiles) { this.profiles = profiles; }

    @GetMapping("/{id}")
    public ProfileResponse getById(@PathVariable Long id) {
        return profiles.getById(id);
    }

    @GetMapping("/profile/{username}")
    public ProfileResponse getByUsername(@PathVariable String username) {
        return profiles.getByUsername(username);
    }

    @PutMapping("/profile")
    public ProfileResponse updateOwnProfile(@AuthenticationPrincipal AuthenticatedUser principal,
                                            @Valid @RequestBody UpdateProfileRequest request) {
        return profiles.updateOwnProfile(principal, request);
    }
}
