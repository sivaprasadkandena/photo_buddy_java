package com.photobuddy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "user_locations",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_locations_user", columnNames = "user_id"),
        indexes = @Index(name = "idx_locations_enabled_coordinates",
                columnList = "location_enabled, latitude, longitude"))
public class UserLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal accuracy;

    @Column(name = "location_enabled", nullable = false)
    private boolean locationEnabled;

    @Column(nullable = false)
    private Instant updatedAt;

    protected UserLocation() {}

    public UserLocation(User user, BigDecimal latitude, BigDecimal longitude, BigDecimal accuracy) {
        this.user = user;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
        this.locationEnabled = false;
    }

    @PrePersist
    @PreUpdate
    void updateTimestamp() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public BigDecimal getLatitude() { return latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public BigDecimal getAccuracy() { return accuracy; }
    public boolean isLocationEnabled() { return locationEnabled; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void updateCoordinates(BigDecimal latitude, BigDecimal longitude, BigDecimal accuracy) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
    }
    public void setLocationEnabled(boolean locationEnabled) { this.locationEnabled = locationEnabled; }
}
