package com.photobuddy.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.photobuddy.security.AuthenticatedUser;
import com.photobuddy.security.JwtTokenService;
import com.photobuddy.service.impl.ChatService;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class ChatWebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final ChatChannelInterceptor chatInterceptor;
    @Value("${app.cors.allowed-origins}") private String allowedOrigins;
    public ChatWebSocketConfig(ChatChannelInterceptor chatInterceptor) { this.chatInterceptor = chatInterceptor; }
    @Override public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }
    @Override public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim).filter(value -> !value.isEmpty()).toArray(String[]::new));
    }
    @Override public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(chatInterceptor);
    }
}

@org.springframework.stereotype.Component
class ChatChannelInterceptor implements ChannelInterceptor {
    private static final Pattern CHAT_TOPIC = Pattern.compile("^/topic/chat/(\\d+)(?:/typing)?$");
    private final JwtTokenService tokens;
    private final UserDetailsService users;
    private final ChatService chats;
    private final ObjectMapper mapper;
    ChatChannelInterceptor(JwtTokenService tokens, UserDetailsService users, ChatService chats, ObjectMapper mapper) {
        this.tokens = tokens; this.users = users; this.chats = chats; this.mapper = mapper;
    }

    @Override public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) return message;
        if (accessor.getCommand() == StompCommand.CONNECT) {
            authenticate(accessor);
            return message;
        }
        if (accessor.getCommand() == StompCommand.SUBSCRIBE) {
            String destination = accessor.getDestination();
            if ("/user/queue/errors".equals(destination)) {
                requireAuthenticated(accessor, message);
                return message;
            }
            Matcher matcher = destination == null ? null : CHAT_TOPIC.matcher(destination);
            if (matcher == null || !matcher.matches()) throw new MessageDeliveryException(message, "Subscription destination is not allowed");
            AuthenticatedUser user = requireAuthenticated(accessor, message);
            chats.requireMember(Long.valueOf(matcher.group(1)), user.id());
            return message;
        }
        if (accessor.getCommand() == StompCommand.SEND) {
            AuthenticatedUser user = requireAuthenticated(accessor, message);
            String destination = accessor.getDestination();
            if (!"/app/chat.send".equals(destination) && !"/app/chat.typing".equals(destination)) {
                throw new MessageDeliveryException(message, "Send destination is not allowed");
            }
            try {
                byte[] payload = (byte[]) message.getPayload();
                if ("/app/chat.send".equals(destination)) {
                    chats.requireMember(mapper.readValue(payload, com.photobuddy.dto.chat.ChatSendRequest.class).roomId(), user.id());
                } else {
                    chats.requireMember(mapper.readValue(payload, com.photobuddy.dto.chat.ChatTypingRequest.class).roomId(), user.id());
                }
            } catch (Exception exception) {
                if (exception instanceof MessageDeliveryException deliveryException) throw deliveryException;
                throw new MessageDeliveryException(message, "Chat message is invalid or unauthorized", exception);
            }
        }
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) throw new MessageDeliveryException("A bearer token is required for chat");
        try {
            String username = tokens.parse(header.substring(7)).get("username", String.class);
            UserDetails userDetails = users.loadUserByUsername(username);
            if (!(userDetails instanceof AuthenticatedUser user) || !user.isEnabled()) {
                throw new MessageDeliveryException("Chat account is unavailable");
            }
            accessor.setUser(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        } catch (MessageDeliveryException exception) { throw exception; }
        catch (RuntimeException exception) { throw new MessageDeliveryException("Chat authentication failed"); }
    }

    private AuthenticatedUser requireAuthenticated(StompHeaderAccessor accessor, Message<?> message) {
        if (accessor.getUser() instanceof UsernamePasswordAuthenticationToken authentication
                && authentication.getPrincipal() instanceof AuthenticatedUser user) return user;
        throw new MessageDeliveryException(message, "Authentication is required for chat");
    }
}
