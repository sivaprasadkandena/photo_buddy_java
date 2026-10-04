package com.photobuddy.repository;

public interface NearbyUserProjection {
    Long getUserId();
    String getFirstName();
    String getLastName();
    String getUsername();
    String getProfilePicture();
    String getBio();
    Boolean getPhotographer();
    Double getDistanceKm();
    Double getLatitude();
    Double getLongitude();
}
