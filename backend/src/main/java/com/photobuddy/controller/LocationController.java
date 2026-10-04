package com.photobuddy.controller;

import com.photobuddy.dto.location.LocationResponse;
import com.photobuddy.dto.location.LocationToggleRequest;
import com.photobuddy.dto.location.LocationUpdateRequest;
import com.photobuddy.dto.location.NearbyUserResponse;
import com.photobuddy.security.AuthenticatedUser;
import com.photobuddy.service.impl.LocationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
public class LocationController {
    private final LocationService locations;

    public LocationController(LocationService locations) { this.locations = locations; }

    @PutMapping({"/api/locations/update", "/api/v1/locations/update"})
    public LocationResponse update(@AuthenticationPrincipal AuthenticatedUser principal,
                                   @Valid @RequestBody LocationUpdateRequest request) {
        return locations.update(principal.id(), request);
    }

    @GetMapping({"/api/locations/my-location", "/api/v1/locations/my-location"})
    public LocationResponse myLocation(@AuthenticationPrincipal AuthenticatedUser principal) {
        return locations.getMine(principal.id());
    }

    @PutMapping({"/api/locations/toggle", "/api/v1/locations/toggle"})
    public LocationResponse toggle(@AuthenticationPrincipal AuthenticatedUser principal,
                                   @Valid @RequestBody LocationToggleRequest request) {
        return locations.toggle(principal.id(), request.locationEnabled());
    }

    @GetMapping({"/api/users/nearby", "/api/v1/users/nearby"})
    public List<NearbyUserResponse> nearby(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
            @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
            @RequestParam(defaultValue = "5") @DecimalMin("0.1") @DecimalMax("100.0") double radius) {
        return locations.findNearby(principal.id(), latitude.doubleValue(), longitude.doubleValue(), radius);
    }
}
