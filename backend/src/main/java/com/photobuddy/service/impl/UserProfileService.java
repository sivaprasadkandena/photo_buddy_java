package com.photobuddy.service.impl;

import com.photobuddy.dto.user.ProfileResponse;
import com.photobuddy.dto.user.UpdateProfileRequest;
import com.photobuddy.entity.User;
import com.photobuddy.repository.UserRepository;
import com.photobuddy.repository.BuddyMatchRepository;
import com.photobuddy.repository.PostRepository;
import com.photobuddy.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserProfileService {
    private final UserRepository users;
    private final BuddyMatchRepository matches;
    private final PostRepository posts;

    public UserProfileService(UserRepository users, BuddyMatchRepository matches, PostRepository posts) {
        this.users = users;
        this.matches = matches;
        this.posts = posts;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getById(Long id) {
        return users.findById(id).map(this::toProfile).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User profile was not found"));
    }

    @Transactional(readOnly = true)
    public ProfileResponse getByUsername(String username) {
        return users.findByUsername(username.trim().toLowerCase()).map(this::toProfile).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User profile was not found"));
    }

    @Transactional
    public ProfileResponse updateOwnProfile(AuthenticatedUser principal, UpdateProfileRequest request) {
        User user = users.findById(principal.id()).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User profile was not found"));
        if (!user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account is disabled");
        }
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setBio(blankToNull(request.bio()));
        user.setGender(request.gender());
        user.setPhotographer(request.isPhotographer());
        user.setProfilePicture(blankToNull(request.profilePicture()));
        return toProfile(user);
    }

    private ProfileResponse toProfile(User user) {
        return new ProfileResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getUsername(),
                user.getGender(), user.getBio(), user.getProfilePicture(), user.isPhotographer(),
                posts.countByUserId(user.getId()), matches.countByUser1IdOrUser2Id(user.getId(), user.getId()), user.getCreatedAt());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
