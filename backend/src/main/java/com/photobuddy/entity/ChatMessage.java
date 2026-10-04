package com.photobuddy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "messages", indexes = {
        @Index(name = "idx_messages_room_time", columnList = "chat_room_id, created_at"),
        @Index(name = "idx_messages_room_read", columnList = "chat_room_id, is_read")
})
public class ChatMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom room;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "sender_id", nullable = false)
    private User sender;
    @Column(length = 4000)
    private String text;
    @Column(length = 2048)
    private String imageUrl;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant timestamp;
    @Column(name = "is_read", nullable = false)
    private boolean isRead;
    protected ChatMessage() {}
    public ChatMessage(ChatRoom room, User sender, String text, String imageUrl) {
        this.room = room; this.sender = sender; this.text = text; this.imageUrl = imageUrl;
    }
    @PrePersist void onCreate() { timestamp = Instant.now(); }
    public Long getId() { return id; }
    public ChatRoom getRoom() { return room; }
    public User getSender() { return sender; }
    public String getText() { return text; }
    public String getImageUrl() { return imageUrl; }
    public Instant getTimestamp() { return timestamp; }
    public boolean isRead() { return isRead; }
    public void markRead() { isRead = true; }
}
