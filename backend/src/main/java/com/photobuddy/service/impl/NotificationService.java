package com.photobuddy.service.impl;

import com.photobuddy.dto.notification.NotificationResponse;
import com.photobuddy.entity.Notification;
import com.photobuddy.entity.NotificationType;
import com.photobuddy.entity.User;
import com.photobuddy.repository.NotificationRepository;
import com.photobuddy.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotificationService {
    private final NotificationRepository notifications;
    private final UserRepository users;
    private final SimpMessagingTemplate messaging;

    public NotificationService(NotificationRepository notifications, UserRepository users, SimpMessagingTemplate messaging) {
        this.notifications = notifications;
        this.users = users;
        this.messaging = messaging;
    }

    @Transactional
    public NotificationResponse create(Long recipientId, Long senderId, NotificationType type, Long referenceId) {
        User recipient = getEnabledUser(recipientId);
        User sender = getEnabledUser(senderId);
        Notification notification = notifications.save(new Notification(recipient, sender, type,
                buildMessage(sender, type), referenceId));
        NotificationResponse response = toResponse(notification);
        messaging.convertAndSendToUser(recipient.getUsername(), "/queue/notifications", response);
        return response;
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> list(Long recipientId, int page, int size) {
        return notifications.findAllByRecipientIdOrderByCreatedAtDesc(recipientId, PageRequest.of(page, size))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long recipientId) {
        return notifications.countByRecipientIdAndReadFalse(recipientId);
    }

    @Transactional
    public NotificationResponse markRead(Long recipientId, Long notificationId) {
        Notification notification = notifications.findByIdAndRecipientId(notificationId, recipientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        notification.markRead();
        return toResponse(notification);
    }

    @Transactional
    public int markAllRead(Long recipientId) {
        return notifications.markAllRead(recipientId);
    }

    public NotificationResponse notifyBuddyRequest(Long recipientId, Long senderId, Long requestId) {
        return create(recipientId, senderId, NotificationType.BUDDY_REQUEST, requestId);
    }

    public NotificationResponse notifyBuddyAccepted(Long recipientId, Long senderId, Long requestId) {
        return create(recipientId, senderId, NotificationType.BUDDY_ACCEPTED, requestId);
    }

    public NotificationResponse notifyNewMessage(Long recipientId, Long senderId, Long roomId, Long messageId) {
        return create(recipientId, senderId, NotificationType.NEW_MESSAGE, roomId != null ? roomId : messageId);
    }

    public NotificationResponse notifyPostLike(Long recipientId, Long senderId, Long postId) {
        return create(recipientId, senderId, NotificationType.POST_LIKE, postId);
    }

    public NotificationResponse notifyPostComment(Long recipientId, Long senderId, Long postId) {
        return create(recipientId, senderId, NotificationType.POST_COMMENT, postId);
    }

    private NotificationResponse toResponse(Notification notification) {
        User sender = notification.getSender();
        return new NotificationResponse(notification.getId(),
                new NotificationResponse.NotificationUser(sender.getId(), sender.getUsername(), sender.getFirstName(),
                        sender.getLastName(), sender.getProfilePicture()),
                notification.getType(),
                notification.getMessage(),
                notification.getReferenceId(),
                notification.isRead(),
                notification.getCreatedAt());
    }

    private String buildMessage(User sender, NotificationType type) {
        String actor = sender.getUsername();
        return switch (type) {
            case BUDDY_REQUEST -> actor + " sent you a buddy request";
            case BUDDY_ACCEPTED -> actor + " accepted your buddy request";
            case NEW_MESSAGE -> actor + " sent you a message";
            case POST_LIKE -> actor + " liked your post";
            case POST_COMMENT -> actor + " commented on your post";
        };
    }

    private User getEnabledUser(Long userId) {
        return users.findById(userId).filter(User::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
