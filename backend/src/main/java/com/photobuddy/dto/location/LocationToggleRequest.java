package com.photobuddy.dto.location;

import jakarta.validation.constraints.NotNull;

public record LocationToggleRequest(@NotNull Boolean locationEnabled) {}
