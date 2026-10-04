package com.photobuddy.dto.chat;

import java.time.Instant;

public record ChatMessageResponse(Long id, Long roomId, ChatUserResponse sender, String text, String imageUrl,
                                  Instant timestamp, boolean isRead) {}
