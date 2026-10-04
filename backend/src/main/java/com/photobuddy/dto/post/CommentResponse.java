package com.photobuddy.dto.post;

import java.time.Instant;

public record CommentResponse(Long id, PostUserResponse user, String content, Instant createdAt) {}
