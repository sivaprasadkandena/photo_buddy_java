package com.photobuddy.dto.chat;

import java.time.Instant;

public record ChatRoomResponse(Long roomId, ChatUserResponse buddy, Instant createdAt, Instant updatedAt) {}
