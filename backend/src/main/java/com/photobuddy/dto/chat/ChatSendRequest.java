package com.photobuddy.dto.chat;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChatSendRequest(@NotNull Long roomId, @Size(max = 4000) String text) {}
