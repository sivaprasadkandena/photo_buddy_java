package com.photobuddy.service.impl;

import com.photobuddy.dto.location.LocationResponse;
import com.photobuddy.dto.location.LocationUpdateRequest;
import com.photobuddy.dto.location.NearbyUserResponse;
import com.photobuddy.entity.User;
import com.photobuddy.entity.UserLocation;
import com.photobuddy.repository.NearbyUserProjection;
import com.photobuddy.repository.UserLocationRepository;
import com.photobuddy.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LocationService {
    private static final double EARTH_RADIUS_KM = 6371.0088;
    private static final int MAX_NEARBY_RESULTS = 100;
    private final UserLocationRepository locations;
    private final UserRepository users;

    public LocationService(UserLocationRepository locations, UserRepository users) {
        this.locations = locations;
        this.users = users;
    }

    @Transactional
    public LocationResponse update(Long userId, LocationUpdateRequest request) {
        User user = getUser(userId);
        UserLocation location = locations.findByUserId(userId).orElseGet(
                () -> new UserLocation(user, request.latitude(), request.longitude(), request.accuracy()));
        location.updateCoordinates(request.latitude(), request.longitude(), request.accuracy());
        location.setLocationEnabled(true);
        return toResponse(locations.save(location));
    }

    @Transactional(readOnly = true)
    public LocationResponse getMine(Long userId) {
        return locations.findByUserId(userId).map(this::toResponse).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No location has been saved"));
    }

    @Transactional
    public LocationResponse toggle(Long userId, boolean enabled) {
        UserLocation location = locations.findByUserId(userId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Update your browser location before enabling sharing"));
        location.setLocationEnabled(enabled);
        return toResponse(locations.save(location));
    }

    @Transactional(readOnly = true)
    public List<NearbyUserResponse> findNearby(Long currentUserId, double latitude, double longitude,
                                               double radiusKm) {
        BoundingBox box = boundingBox(latitude, longitude, radiusKm);
        return locations.findNearby(currentUserId, latitude, longitude,
                        box.minLatitude(), box.maxLatitude(), box.minLongitude(), box.maxLongitude(),
                        box.wrapLongitude(), radiusKm, MAX_NEARBY_RESULTS)
                .stream().map(this::toNearbyResponse).toList();
    }

    static double haversineKm(double latitude1, double longitude1, double latitude2, double longitude2) {
        double latDelta = Math.toRadians(latitude2 - latitude1);
        double lonDelta = Math.toRadians(longitude2 - longitude1);
        double a = Math.pow(Math.sin(latDelta / 2), 2)
                + Math.cos(Math.toRadians(latitude1)) * Math.cos(Math.toRadians(latitude2))
                * Math.pow(Math.sin(lonDelta / 2), 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(Math.min(1, a)));
    }

    private User getUser(Long userId) {
        return users.findById(userId).filter(User::isEnabled).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User was not found"));
    }

    private LocationResponse toResponse(UserLocation location) {
        return new LocationResponse(location.getLatitude(), location.getLongitude(), location.getAccuracy(),
                location.isLocationEnabled(), location.getUpdatedAt());
    }

    private NearbyUserResponse toNearbyResponse(NearbyUserProjection user) {
        return new NearbyUserResponse(user.getUserId(), user.getFirstName(), user.getLastName(), user.getUsername(),
                user.getProfilePicture(), user.getBio(), Boolean.TRUE.equals(user.getPhotographer()),
                round(user.getDistanceKm(), 2), round(user.getLatitude(), 2), round(user.getLongitude(), 2));
    }

    private static double round(double value, int places) {
        return BigDecimal.valueOf(value).setScale(places, RoundingMode.HALF_UP).doubleValue();
    }

    private static BoundingBox boundingBox(double latitude, double longitude, double radiusKm) {
        double latitudeDelta = radiusKm / 111.32;
        double minLatitude = Math.max(-90, latitude - latitudeDelta);
        double maxLatitude = Math.min(90, latitude + latitudeDelta);
        double longitudeScale = Math.cos(Math.toRadians(latitude));
        double longitudeDelta = Math.abs(longitudeScale) < 1.0e-8
                ? 180 : Math.min(180, radiusKm / (111.32 * Math.abs(longitudeScale)));
        if (longitudeDelta >= 180) return new BoundingBox(minLatitude, maxLatitude, -180, 180, false);

        double rawMinLongitude = longitude - longitudeDelta;
        double rawMaxLongitude = longitude + longitudeDelta;
        if (rawMinLongitude < -180) {
            return new BoundingBox(minLatitude, maxLatitude, rawMinLongitude + 360, rawMaxLongitude, true);
        }
        if (rawMaxLongitude > 180) {
            return new BoundingBox(minLatitude, maxLatitude, rawMinLongitude, rawMaxLongitude - 360, true);
        }
        return new BoundingBox(minLatitude, maxLatitude, rawMinLongitude, rawMaxLongitude, false);
    }

    private record BoundingBox(double minLatitude, double maxLatitude, double minLongitude,
                               double maxLongitude, boolean wrapLongitude) {}
}
