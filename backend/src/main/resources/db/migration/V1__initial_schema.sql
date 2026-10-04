CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    first_name VARCHAR(60) NOT NULL,
    last_name VARCHAR(60) NOT NULL,
    username VARCHAR(30) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password VARCHAR(60) NOT NULL,
    gender VARCHAR(24) NOT NULL,
    bio VARCHAR(500),
    profile_picture VARCHAR(2048),
    photographer BOOLEAN NOT NULL,
    enabled BOOLEAN NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE refresh_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    revoked BOOLEAN NOT NULL,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE posts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    image_url VARCHAR(2048) NOT NULL,
    caption VARCHAR(2200),
    style VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_posts PRIMARY KEY (id),
    CONSTRAINT fk_posts_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    INDEX idx_posts_created (created_at, id),
    INDEX idx_posts_user_created (user_id, created_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE post_likes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    CONSTRAINT pk_post_likes PRIMARY KEY (id),
    CONSTRAINT uk_post_like_user UNIQUE (post_id, user_id),
    CONSTRAINT fk_post_likes_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE RESTRICT,
    CONSTRAINT fk_post_likes_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    INDEX idx_post_likes_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE comments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content VARCHAR(1000) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_comments PRIMARY KEY (id),
    CONSTRAINT fk_comments_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE RESTRICT,
    CONSTRAINT fk_comments_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    INDEX idx_comments_post_created (post_id, created_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_locations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    latitude DECIMAL(10,7) NOT NULL,
    longitude DECIMAL(10,7) NOT NULL,
    accuracy DECIMAL(10,2) NOT NULL,
    location_enabled BOOLEAN NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_user_locations PRIMARY KEY (id),
    CONSTRAINT uk_user_locations_user UNIQUE (user_id),
    CONSTRAINT fk_user_locations_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    INDEX idx_locations_enabled_coordinates (location_enabled, latitude, longitude)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE buddy_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    pair_low_id BIGINT NOT NULL,
    pair_high_id BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_buddy_requests PRIMARY KEY (id),
    CONSTRAINT uk_buddy_request_pair UNIQUE (pair_low_id, pair_high_id),
    CONSTRAINT chk_buddy_request_pair CHECK (pair_low_id < pair_high_id AND sender_id <> receiver_id),
    CONSTRAINT fk_buddy_request_sender FOREIGN KEY (sender_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_buddy_request_receiver FOREIGN KEY (receiver_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_buddy_request_pair_low FOREIGN KEY (pair_low_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_buddy_request_pair_high FOREIGN KEY (pair_high_id) REFERENCES users (id) ON DELETE RESTRICT,
    INDEX idx_buddy_requests_receiver_status (receiver_id, status, updated_at),
    INDEX idx_buddy_requests_sender_status (sender_id, status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE matches (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user1_id BIGINT NOT NULL,
    user2_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_matches PRIMARY KEY (id),
    CONSTRAINT uk_matches_user_pair UNIQUE (user1_id, user2_id),
    CONSTRAINT chk_matches_canonical_pair CHECK (user1_id < user2_id),
    CONSTRAINT fk_matches_user1 FOREIGN KEY (user1_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_matches_user2 FOREIGN KEY (user2_id) REFERENCES users (id) ON DELETE RESTRICT,
    INDEX idx_matches_user1 (user1_id, created_at),
    INDEX idx_matches_user2 (user2_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE chat_rooms (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user1_id BIGINT NOT NULL,
    user2_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_chat_rooms PRIMARY KEY (id),
    CONSTRAINT uk_chat_room_pair UNIQUE (user1_id, user2_id),
    CONSTRAINT chk_chat_room_canonical_pair CHECK (user1_id < user2_id),
    CONSTRAINT fk_chat_rooms_user1 FOREIGN KEY (user1_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_chat_rooms_user2 FOREIGN KEY (user2_id) REFERENCES users (id) ON DELETE RESTRICT,
    INDEX idx_chat_rooms_user1 (user1_id, updated_at),
    INDEX idx_chat_rooms_user2 (user2_id, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE messages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    chat_room_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    text VARCHAR(4000),
    image_url VARCHAR(2048),
    created_at DATETIME(6) NOT NULL,
    is_read BOOLEAN NOT NULL,
    CONSTRAINT pk_messages PRIMARY KEY (id),
    CONSTRAINT fk_messages_room FOREIGN KEY (chat_room_id) REFERENCES chat_rooms (id) ON DELETE RESTRICT,
    CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id) REFERENCES users (id) ON DELETE RESTRICT,
    INDEX idx_messages_room_time (chat_room_id, created_at, id),
    INDEX idx_messages_room_read (chat_room_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    recipient_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    type VARCHAR(24) NOT NULL,
    message VARCHAR(255) NOT NULL,
    reference_id BIGINT,
    is_read BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_notifications PRIMARY KEY (id),
    CONSTRAINT fk_notifications_recipient FOREIGN KEY (recipient_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_notifications_sender FOREIGN KEY (sender_id) REFERENCES users (id) ON DELETE RESTRICT,
    INDEX idx_notifications_recipient_created (recipient_id, created_at),
    INDEX idx_notifications_recipient_unread (recipient_id, is_read, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
