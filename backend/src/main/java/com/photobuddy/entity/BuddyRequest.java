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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(name = "buddy_requests",
        uniqueConstraints = @UniqueConstraint(name = "uk_buddy_request_pair", columnNames = {"pair_low_id", "pair_high_id"}),
        indexes = {
                @Index(name = "idx_buddy_requests_receiver_status", columnList = "receiver_id, status, updated_at"),
                @Index(name = "idx_buddy_requests_sender_status", columnList = "sender_id, status, updated_at")
        })
public class BuddyRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pair_low_id", nullable = false)
    private User pairLow;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pair_high_id", nullable = false)
    private User pairHigh;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private BuddyRequestStatus status = BuddyRequestStatus.PENDING;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected BuddyRequest() {}

    public BuddyRequest(User sender, User receiver, User pairLow, User pairHigh) {
        this.sender = sender;
        this.receiver = receiver;
        this.pairLow = pairLow;
        this.pairHigh = pairHigh;
        this.status = BuddyRequestStatus.PENDING;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public User getSender() { return sender; }
    public User getReceiver() { return receiver; }
    public User getPairLow() { return pairLow; }
    public User getPairHigh() { return pairHigh; }
    public BuddyRequestStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void reopen(User sender, User receiver) {
        this.sender = sender;
        this.receiver = receiver;
        this.status = BuddyRequestStatus.PENDING;
    }

    public void accept() { this.status = BuddyRequestStatus.ACCEPTED; }
    public void reject() { this.status = BuddyRequestStatus.REJECTED; }
    public void cancel() { this.status = BuddyRequestStatus.CANCELLED; }
}
