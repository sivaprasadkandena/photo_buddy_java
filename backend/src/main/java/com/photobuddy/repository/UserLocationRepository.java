package com.photobuddy.repository;

import com.photobuddy.entity.UserLocation;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserLocationRepository extends JpaRepository<UserLocation, Long> {
    Optional<UserLocation> findByUserId(Long userId);
    List<UserLocation> findAllByUserIdIn(Collection<Long> userIds);

    @Query(value = """
            SELECT * FROM (
              SELECT u.id AS userId, u.first_name AS firstName, u.last_name AS lastName,
                   u.username AS username, u.profile_picture AS profilePicture, u.bio AS bio,
                   u.photographer AS photographer,
                   2.0 * 6371.0088 * ASIN(SQRT(LEAST(1.0,
                       POWER(SIN(RADIANS(loc.latitude - :latitude) / 2.0), 2.0) +
                       COS(RADIANS(:latitude)) * COS(RADIANS(loc.latitude)) *
                       POWER(SIN(RADIANS(loc.longitude - :longitude) / 2.0), 2.0)
                   ))) AS distanceKm,
                   loc.latitude AS latitude, loc.longitude AS longitude
              FROM user_locations loc
              JOIN users u ON u.id = loc.user_id
              WHERE loc.location_enabled = TRUE
                AND u.enabled = TRUE
                AND u.id <> :currentUserId
                AND loc.latitude BETWEEN :minLatitude AND :maxLatitude
                AND ((:wrapLongitude = FALSE AND loc.longitude BETWEEN :minLongitude AND :maxLongitude)
                     OR (:wrapLongitude = TRUE AND (loc.longitude >= :minLongitude OR loc.longitude <= :maxLongitude)))
            ) nearby
            WHERE distanceKm <= :radiusKm
            ORDER BY distanceKm ASC
            LIMIT :maxResults
            """, nativeQuery = true)
    List<NearbyUserProjection> findNearby(
            @Param("currentUserId") Long currentUserId,
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("minLatitude") double minLatitude,
            @Param("maxLatitude") double maxLatitude,
            @Param("minLongitude") double minLongitude,
            @Param("maxLongitude") double maxLongitude,
            @Param("wrapLongitude") boolean wrapLongitude,
            @Param("radiusKm") double radiusKm,
            @Param("maxResults") int maxResults);
}
