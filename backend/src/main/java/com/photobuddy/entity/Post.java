package com.photobuddy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "posts", indexes = @Index(name = "idx_posts_user_created", columnList = "user_id, created_at"))
public class Post {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 2048)
    private String imageUrl;

    @Column(length = 2200)
    private String caption;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostStyle style;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Post() {}

    public Post(User user, String imageUrl, String caption, PostStyle style) {
        this.user = user;
        this.imageUrl = imageUrl;
        this.caption = caption;
        this.style = style;
    }

    @PrePersist void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getImageUrl() { return imageUrl; }
    public String getCaption() { return caption; }
    public PostStyle getStyle() { return style; }
    public Instant getCreatedAt() { return createdAt; }
    public void setImageUrl(String value) { imageUrl = value; }
    public void setCaption(String value) { caption = value; }
    public void setStyle(PostStyle value) { style = value; }
}
