package com.photobuddy.controller;

import com.photobuddy.dto.notification.NotificationResponse;
import com.photobuddy.dto.common.PageResponse;
import com.photobuddy.security.AuthenticatedUser;
import com.photobuddy.service.impl.NotificationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/notifications", "/api/v1/notifications"})
@Validated
public class NotificationController {
    private final NotificationService notifications;

    public NotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    public PageResponse<NotificationResponse> list(@AuthenticationPrincipal AuthenticatedUser user,
                                                  @RequestParam(defaultValue = "0") @Min(0) int page,
                                                  @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return PageResponse.from(notifications.list(user.id(), page, size));
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal AuthenticatedUser user) {
        return Map.of("count", notifications.unreadCount(user.id()));
    }

    @PutMapping("/{notificationId}/read")
    public NotificationResponse markRead(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long notificationId) {
        return notifications.markRead(user.id(), notificationId);
    }

    @PutMapping("/read-all")
    public Map<String, Integer> markAllRead(@AuthenticationPrincipal AuthenticatedUser user) {
        return Map.of("updated", notifications.markAllRead(user.id()));
    }
}
