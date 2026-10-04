package com.photobuddy.dto.location;

import java.math.BigDecimal;
import java.time.Instant;

public record LocationResponse(BigDecimal latitude, BigDecimal longitude, BigDecimal accuracy,
                               boolean locationEnabled, Instant updatedAt) {}
