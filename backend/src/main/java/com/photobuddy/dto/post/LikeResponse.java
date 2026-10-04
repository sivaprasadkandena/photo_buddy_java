package com.photobuddy.dto.post;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LikeResponse(Long postId, long likeCount, boolean likedByCurrentUser) {
	@JsonProperty("likedByMe")
	public boolean likedByMe() { return likedByCurrentUser; }
}
