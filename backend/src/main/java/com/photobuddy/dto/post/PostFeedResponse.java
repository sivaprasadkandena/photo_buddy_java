package com.photobuddy.dto.post;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.photobuddy.entity.PostStyle;
import java.time.Instant;

public record PostFeedResponse(Long postId, PostUserResponse user, String imageUrl, String caption,
                               PostStyle style, long likeCount, long commentCount, Instant createdAt,
                               boolean likedByCurrentUser) {
    @JsonProperty("likedByMe")
    public boolean likedByMe() { return likedByCurrentUser; }
}
