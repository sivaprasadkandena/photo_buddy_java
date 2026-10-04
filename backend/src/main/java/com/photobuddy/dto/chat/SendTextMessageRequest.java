package com.photobuddy.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendTextMessageRequest(@NotBlank @Size(max = 4000) String text) {}
