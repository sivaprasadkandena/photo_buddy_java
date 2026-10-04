package com.photobuddy.controller;

import com.photobuddy.dto.chat.ChatMessageResponse;
import com.photobuddy.dto.chat.ChatRoomResponse;
import com.photobuddy.dto.chat.SendTextMessageRequest;
import com.photobuddy.dto.common.PageResponse;
import com.photobuddy.security.AuthenticatedUser;
import com.photobuddy.service.impl.ChatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping({"/api/chat/rooms", "/api/v1/chat/rooms"})
@Validated
public class ChatController {
    private final ChatService chats;
    private final SimpMessagingTemplate messaging;
    public ChatController(ChatService chats, SimpMessagingTemplate messaging) { this.chats = chats; this.messaging = messaging; }

    @GetMapping
    public List<ChatRoomResponse> rooms(@AuthenticationPrincipal AuthenticatedUser user) { return chats.getRooms(user.id()); }

    @PostMapping("/{buddyId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatRoomResponse open(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long buddyId) {
        return chats.openRoom(user.id(), buddyId);
    }

    @GetMapping("/{roomId}")
    public ChatRoomResponse getRoom(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long roomId) {
        return chats.getRoom(roomId, user.id());
    }

    @PostMapping("/{roomId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatMessageResponse sendText(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long roomId,
                                        @Valid @RequestBody SendTextMessageRequest request) {
        ChatMessageResponse message = chats.sendText(roomId, user.getUsername(), request.text());
        messaging.convertAndSend("/topic/chat/" + roomId, message);
        return message;
    }

    @GetMapping("/{roomId}/messages")
    public PageResponse<ChatMessageResponse> history(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long roomId,
                                                     @RequestParam(defaultValue = "0") @Min(0) int page,
                                                     @RequestParam(defaultValue = "30") @Min(1) @Max(100) int size) {
        return PageResponse.from(chats.getHistory(roomId, user.id(), page, size));
    }

    @PostMapping(value = "/{roomId}/messages/image", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatMessageResponse sendImage(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long roomId,
                                         @RequestParam("image") MultipartFile image,
                                         @RequestParam(value = "text", required = false) String text) {
        ChatMessageResponse message = chats.sendImage(roomId, user.id(), text, image);
        messaging.convertAndSend("/topic/chat/" + roomId, message);
        return message;
    }

    @PutMapping("/{roomId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long roomId) {
        chats.markRead(roomId, user.id());
    }
}
