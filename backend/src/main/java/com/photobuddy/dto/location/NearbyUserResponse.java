package com.photobuddy.dto.location;

public record NearbyUserResponse(Long userId, String firstName, String lastName, String username,
                                 String profilePicture, String bio, boolean isPhotographer,
                                 double distanceKm, double approximateLatitude, double approximateLongitude) {}
