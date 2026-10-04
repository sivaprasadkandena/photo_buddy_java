package com.photobuddy.controller;

import com.photobuddy.dto.chat.ChatErrorResponse;
import com.photobuddy.dto.chat.ChatMessageResponse;
import com.photobuddy.dto.chat.ChatSendRequest;
import com.photobuddy.dto.chat.ChatTypingEvent;
import com.photobuddy.dto.chat.ChatTypingRequest;
import com.photobuddy.service.impl.ChatService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ChatWebSocketController {
    private final ChatService chats;
    private final SimpMessagingTemplate messaging;
    public ChatWebSocketController(ChatService chats, SimpMessagingTemplate messaging) {
        this.chats = chats; this.messaging = messaging;
    }

    @MessageMapping("/chat.send")
    public void send(@Valid ChatSendRequest request, Principal principal) {
        ChatMessageResponse message = chats.sendText(request.roomId(), principal.getName(), request.text());
        messaging.convertAndSend("/topic/chat/" + request.roomId(), message);
    }

    @MessageMapping("/chat.typing")
    public void typing(@Valid ChatTypingRequest request, Principal principal) {
        chats.requireMemberByUsername(request.roomId(), principal.getName());
        messaging.convertAndSend("/topic/chat/" + request.roomId() + "/typing",
                new ChatTypingEvent(principal.getName(), request.typing()));
    }

    @MessageExceptionHandler
    public void onChatError(Exception exception, Principal principal) {
        if (principal != null) messaging.convertAndSendToUser(principal.getName(), "/queue/errors",
                new ChatErrorResponse("Message could not be sent to this chat."));
    }
}
