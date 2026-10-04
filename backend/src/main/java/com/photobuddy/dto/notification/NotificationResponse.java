package com.photobuddy.dto.notification;

import com.photobuddy.entity.NotificationType;
import java.time.Instant;

public record NotificationResponse(Long id, NotificationUser sender, NotificationType type, String message,
                                  Long referenceId, boolean isRead, Instant createdAt) {
    public record NotificationUser(Long id, String username, String firstName, String lastName, String profilePicture) {}
}
