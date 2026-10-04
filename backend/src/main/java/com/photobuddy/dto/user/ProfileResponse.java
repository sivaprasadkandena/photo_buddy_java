package com.photobuddy.dto.user;

import com.photobuddy.entity.Gender;
import java.time.Instant;

public record ProfileResponse(Long id, String firstName, String lastName, String username, Gender gender,
                              String bio, String profilePicture, boolean isPhotographer,
                              long postCount, long buddyCount, Instant createdAt) {}
