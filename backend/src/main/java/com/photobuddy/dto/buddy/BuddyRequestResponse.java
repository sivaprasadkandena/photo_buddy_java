package com.photobuddy.dto.buddy;

import com.photobuddy.entity.BuddyRequestStatus;
import java.time.Instant;

public record BuddyRequestResponse(Long id, BuddyUserSummary sender, BuddyUserSummary receiver,
                                   BuddyRequestStatus status, Instant createdAt, Instant updatedAt) {}
