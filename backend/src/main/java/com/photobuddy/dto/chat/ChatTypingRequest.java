package com.photobuddy.dto.chat;

import jakarta.validation.constraints.NotNull;

public record ChatTypingRequest(@NotNull Long roomId, boolean typing) {}
