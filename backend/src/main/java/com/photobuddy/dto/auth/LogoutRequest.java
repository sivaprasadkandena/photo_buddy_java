package com.photobuddy.dto.auth;

import jakarta.validation.constraints.Size;

public record LogoutRequest(@Size(max = 256) String refreshToken) {}
