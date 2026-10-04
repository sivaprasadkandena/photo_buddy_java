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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(name = "matches",
        uniqueConstraints = @UniqueConstraint(name = "uk_matches_user_pair", columnNames = {"user1_id", "user2_id"}),
        indexes = {
                @Index(name = "idx_matches_user1", columnList = "user1_id, created_at"),
                @Index(name = "idx_matches_user2", columnList = "user2_id, created_at")
        })
public class BuddyMatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user1_id", nullable = false)
    private User user1;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user2_id", nullable = false)
    private User user2;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected BuddyMatch() {}

    public BuddyMatch(User user1, User user2) {
        this.user1 = user1;
        this.user2 = user2;
    }

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }

    public Long getId() { return id; }
    public User getUser1() { return user1; }
    public User getUser2() { return user2; }
    public Instant getCreatedAt() { return createdAt; }
}
