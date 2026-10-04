package com.photobuddy.dto.auth;

import java.util.List;

public record AuthUserResponse(Long id, String firstName, String lastName, String username, String email,
                               List<String> roles) {}
