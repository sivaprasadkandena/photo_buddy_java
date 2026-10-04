package com.photobuddy.controller;

import com.photobuddy.dto.post.CommentResponse;
import com.photobuddy.dto.post.CreateCommentRequest;
import com.photobuddy.dto.post.LikeResponse;
import com.photobuddy.dto.post.PostFeedResponse;
import com.photobuddy.dto.common.PageResponse;
import com.photobuddy.entity.PostStyle;
import com.photobuddy.security.AuthenticatedUser;
import com.photobuddy.service.impl.PostService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping({"/api", "/api/v1"})
@Validated
public class PostController {
    private final PostService posts;
    public PostController(PostService posts) { this.posts = posts; }
    @PostMapping(value = "/posts", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public PostFeedResponse create(@AuthenticationPrincipal AuthenticatedUser user, @RequestParam("image") MultipartFile image,
                                   @RequestParam(value = "caption", required = false) String caption,
                                   @RequestParam("style") PostStyle style) {
        return posts.create(user.id(), image, caption, style);
    }
    @GetMapping("/posts")
    public PageResponse<PostFeedResponse> feed(@AuthenticationPrincipal AuthenticatedUser user,
                                               @RequestParam(defaultValue = "0") @Min(0) int page,
                                               @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return PageResponse.from(posts.feed(user.id(), page, size));
    }
    @GetMapping("/posts/{postId}")
    public PostFeedResponse get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long postId) {
        return posts.get(postId, user.id());
    }
    @PutMapping(value = "/posts/{postId}", consumes = "multipart/form-data")
    public PostFeedResponse update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long postId,
                                   @RequestParam(value = "image", required = false) MultipartFile image,
                                   @RequestParam(value = "caption", required = false) String caption,
                                   @RequestParam(value = "style", required = false) PostStyle style) {
        return posts.update(postId, user.id(), image, caption, style);
    }
    @DeleteMapping("/posts/{postId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long postId) { posts.delete(postId, user.id()); }
    @PostMapping("/posts/{postId}/like")
    public LikeResponse like(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long postId) { return posts.like(postId, user.id()); }
    @DeleteMapping("/posts/{postId}/like")
    public LikeResponse unlike(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long postId) { return posts.unlike(postId, user.id()); }
    @GetMapping("/posts/{postId}/comments")
    public PageResponse<CommentResponse> comments(@PathVariable Long postId, @RequestParam(defaultValue = "0") @Min(0) int page,
                                                  @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return PageResponse.from(posts.getComments(postId, page, size));
    }
    @PostMapping("/posts/{postId}/comments") @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse addComment(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long postId,
                                      @Valid @RequestBody CreateCommentRequest request) {
        return posts.addComment(postId, user.id(), request);
    }
    @DeleteMapping("/comments/{commentId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long commentId) {
        posts.deleteComment(commentId, user.id());
    }
}
