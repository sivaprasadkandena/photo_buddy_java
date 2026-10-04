package com.photobuddy.dto.auth;

public record AuthResponse(String tokenType, String accessToken, long accessTokenExpiresIn,
                           String refreshToken, AuthUserResponse user) {}
