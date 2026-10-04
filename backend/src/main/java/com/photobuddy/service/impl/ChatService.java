package com.photobuddy.service.impl;

import com.photobuddy.dto.chat.ChatMessageResponse;
import com.photobuddy.dto.chat.ChatRoomResponse;
import com.photobuddy.dto.chat.ChatUserResponse;
import com.photobuddy.entity.ChatMessage;
import com.photobuddy.entity.ChatRoom;
import com.photobuddy.entity.User;
import com.photobuddy.repository.BuddyMatchRepository;
import com.photobuddy.repository.ChatMessageRepository;
import com.photobuddy.repository.ChatRoomRepository;
import com.photobuddy.repository.UserRepository;
import com.photobuddy.storage.FileStorageService;
import java.util.List;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ChatService {
    private final ChatRoomRepository rooms;
    private final ChatMessageRepository messages;
    private final BuddyMatchRepository matches;
    private final UserRepository users;
    private final FileStorageService storage;
    private final NotificationService notifications;

    public ChatService(ChatRoomRepository rooms, ChatMessageRepository messages, BuddyMatchRepository matches,
                       UserRepository users, FileStorageService storage, @Lazy NotificationService notifications) {
        this.rooms = rooms; this.messages = messages; this.matches = matches; this.users = users; this.storage = storage;
        this.notifications = notifications;
    }

    @Transactional
    public ChatRoomResponse openRoom(Long userId, Long buddyId) {
        if (userId.equals(buddyId)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot chat with yourself");
        User current = getEnabledUser(userId);
        User buddy = getEnabledUser(buddyId);
        long firstId = Math.min(userId, buddyId), secondId = Math.max(userId, buddyId);
        if (!matches.existsByUser1IdAndUser2Id(firstId, secondId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only matched buddies can start a chat");
        }
        ChatRoom room = rooms.findByUser1IdAndUser2Id(firstId, secondId).orElseGet(() -> rooms.save(new ChatRoom(
                userId.equals(firstId) ? current : buddy, userId.equals(firstId) ? buddy : current)));
        return toRoomResponse(room, userId);
    }

    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getRooms(Long userId) {
        return rooms.findAllByUser1IdOrUser2IdOrderByUpdatedAtDesc(userId, userId).stream()
                .map(room -> toRoomResponse(room, userId)).toList();
    }

    @Transactional(readOnly = true)
    public ChatRoomResponse getRoom(Long roomId, Long userId) {
        ChatRoom room = requireMember(roomId, userId);
        return toRoomResponse(room, userId);
    }

    @Transactional(readOnly = true)
    public Page<ChatMessageResponse> getHistory(Long roomId, Long userId, int page, int size) {
        requireMember(roomId, userId);
        return messages.findHistory(roomId, PageRequest.of(page, size));
    }

    @Transactional
    public ChatMessageResponse sendText(Long roomId, String senderUsername, String text) {
        User sender = users.findByUsername(senderUsername).filter(User::isEnabled).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chat sender is unavailable"));
        return saveMessage(requireMember(roomId, sender.getId()), sender, cleanText(text), null);
    }

    @Transactional
    public ChatMessageResponse sendImage(Long roomId, Long senderId, String text, MultipartFile image) {
        User sender = getEnabledUser(senderId);
        ChatRoom room = requireMember(roomId, senderId);
        String cleanText = cleanOptionalText(text);
        String imageUrl = storage.storeImage(image);
        try { return saveMessage(room, sender, cleanText, imageUrl); }
        catch (RuntimeException error) { storage.delete(imageUrl); throw error; }
    }

    @Transactional
    public void markRead(Long roomId, Long userId) {
        requireMember(roomId, userId);
        messages.markIncomingRead(roomId, userId);
    }

    @Transactional(readOnly = true)
    public ChatRoom requireMemberByUsername(Long roomId, String username) {
        User user = users.findByUsername(username).filter(User::isEnabled).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chat sender is unavailable"));
        return requireMember(roomId, user.getId());
    }

    @Transactional(readOnly = true)
    public ChatRoom requireMember(Long roomId, Long userId) {
        ChatRoom room = rooms.findWithUsersById(roomId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat room was not found"));
        if (!room.getUser1().getId().equals(userId) && !room.getUser2().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this chat room");
        }
        return room;
    }

    private ChatMessageResponse saveMessage(ChatRoom room, User sender, String text, String imageUrl) {
        room.recordActivity();
        ChatMessage message = messages.save(new ChatMessage(room, sender, text, imageUrl));
        User recipient = room.getUser1().getId().equals(sender.getId()) ? room.getUser2() : room.getUser1();
        if (!recipient.getId().equals(sender.getId())) {
            notifications.notifyNewMessage(recipient.getId(), sender.getId(), room.getId(), message.getId());
        }
        return new ChatMessageResponse(message.getId(), room.getId(), summary(sender), message.getText(),
                message.getImageUrl(), message.getTimestamp(), message.isRead());
    }

    private ChatRoomResponse toRoomResponse(ChatRoom room, Long userId) {
        User buddy = room.getUser1().getId().equals(userId) ? room.getUser2() : room.getUser1();
        return new ChatRoomResponse(room.getId(), summary(buddy), room.getCreatedAt(), room.getUpdatedAt());
    }

    private User getEnabledUser(Long id) {
        return users.findById(id).filter(User::isEnabled).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User was not found"));
    }
    private String cleanText(String text) {
        String value = cleanOptionalText(text);
        if (value == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message text cannot be empty");
        return value;
    }
    private String cleanOptionalText(String text) {
        if (text == null || text.isBlank()) return null;
        String value = text.trim();
        if (value.length() > 4000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message cannot exceed 4000 characters");
        return value;
    }
    private ChatUserResponse summary(User user) {
        return new ChatUserResponse(user.getId(), user.getUsername(), user.getFirstName(), user.getLastName(), user.getProfilePicture());
    }
}
