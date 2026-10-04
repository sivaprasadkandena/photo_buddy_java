package com.photobuddy.dto.buddy;

import java.time.Instant;

public record BuddyMatchResponse(Long id, BuddyUserSummary user, Double distanceKm, Instant matchedAt) {}
