package com.photobuddy.service.impl;

import com.photobuddy.dto.post.CommentResponse;
import com.photobuddy.dto.post.CreateCommentRequest;
import com.photobuddy.dto.post.LikeResponse;
import com.photobuddy.dto.post.PostFeedResponse;
import com.photobuddy.dto.post.PostUserResponse;
import com.photobuddy.entity.Comment;
import com.photobuddy.entity.Post;
import com.photobuddy.entity.PostLike;
import com.photobuddy.entity.PostStyle;
import com.photobuddy.entity.User;
import com.photobuddy.repository.CommentRepository;
import com.photobuddy.repository.PostLikeRepository;
import com.photobuddy.repository.PostRepository;
import com.photobuddy.repository.UserRepository;
import com.photobuddy.storage.FileStorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PostService {
    private final PostRepository posts;
    private final PostLikeRepository likes;
    private final CommentRepository comments;
    private final UserRepository users;
    private final FileStorageService storage;
    private final NotificationService notifications;

    public PostService(PostRepository posts, PostLikeRepository likes, CommentRepository comments,
                       UserRepository users, FileStorageService storage, NotificationService notifications) {
        this.posts = posts; this.likes = likes; this.comments = comments; this.users = users; this.storage = storage;
        this.notifications = notifications;
    }

    @Transactional
    public PostFeedResponse create(Long userId, MultipartFile image, String caption, PostStyle style) {
        User user = getEnabledUser(userId);
        String imageUrl = storage.storeImage(image);
        try {
            Post post = posts.save(new Post(user, imageUrl, cleanCaption(caption), requireStyle(style)));
            return view(post.getId(), userId);
        } catch (RuntimeException error) {
            storage.delete(imageUrl);
            throw error;
        }
    }

    @Transactional(readOnly = true)
    public Page<PostFeedResponse> feed(Long viewerId, int page, int size) {
        return posts.findFeed(viewerId, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public PostFeedResponse get(Long postId, Long viewerId) { return view(postId, viewerId); }

    @Transactional
    public PostFeedResponse update(Long postId, Long userId, MultipartFile image, String caption, PostStyle style) {
        Post post = getPost(postId);
        requireOwner(post, userId);
        String oldImage = post.getImageUrl();
        String newImage = image == null || image.isEmpty() ? null : storage.storeImage(image);
        try {
            if (newImage != null) post.setImageUrl(newImage);
            if (caption != null) post.setCaption(cleanCaption(caption));
            if (style != null) post.setStyle(style);
            PostFeedResponse response = view(postId, userId);
            if (newImage != null) storage.delete(oldImage);
            return response;
        } catch (RuntimeException error) {
            if (newImage != null) storage.delete(newImage);
            throw error;
        }
    }

    @Transactional
    public void delete(Long postId, Long userId) {
        Post post = getPost(postId);
        requireOwner(post, userId);
        likes.deleteByPostId(postId);
        comments.deleteByPostId(postId);
        posts.delete(post);
        storage.delete(post.getImageUrl());
    }

    @Transactional
    public LikeResponse like(Long postId, Long userId) {
        Post post = getPost(postId);
        User user = getEnabledUser(userId);
        if (likes.existsByPostIdAndUserId(postId, userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already liked this post");
        }
        likes.save(new PostLike(post, user));
        if (!post.getUser().getId().equals(userId)) {
            notifications.notifyPostLike(post.getUser().getId(), userId, postId);
        }
        return new LikeResponse(postId, likes.countByPostId(postId), true);
    }

    @Transactional
    public LikeResponse unlike(Long postId, Long userId) {
        getPost(postId);
        likes.findByPostIdAndUserId(postId, userId).ifPresent(likes::delete);
        return new LikeResponse(postId, likes.countByPostId(postId), false);
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> getComments(Long postId, int page, int size) {
        getPost(postId);
        return comments.findForPost(postId, PageRequest.of(page, size));
    }

    @Transactional
    public CommentResponse addComment(Long postId, Long userId, CreateCommentRequest request) {
        Post post = getPost(postId);
        User user = getEnabledUser(userId);
        Comment comment = comments.save(new Comment(post, user, request.content().trim()));
        if (!post.getUser().getId().equals(userId)) {
            notifications.notifyPostComment(post.getUser().getId(), userId, postId);
        }
        return new CommentResponse(comment.getId(), summary(user), comment.getContent(), comment.getCreatedAt());
    }

    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = comments.findByIdAndUserId(commentId, userId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment was not found"));
        comments.delete(comment);
    }

    private PostFeedResponse view(Long postId, Long viewerId) {
        return posts.findPostView(postId, viewerId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post was not found"));
    }

    private Post getPost(Long id) {
        return posts.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post was not found"));
    }

    private User getEnabledUser(Long id) {
        return users.findById(id).filter(User::isEnabled).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.FORBIDDEN, "This account is disabled or unavailable"));
    }

    private void requireOwner(Post post, Long userId) {
        if (!post.getUser().getId().equals(userId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the post owner can edit or delete it");
    }

    private String cleanCaption(String caption) {
        if (caption == null || caption.isBlank()) return null;
        String value = caption.trim();
        if (value.length() > 2200) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Caption cannot exceed 2200 characters");
        return value;
    }

    private PostStyle requireStyle(PostStyle style) {
        if (style == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a post style");
        return style;
    }

    private PostUserResponse summary(User user) {
        return new PostUserResponse(user.getId(), user.getUsername(), user.getFirstName(), user.getLastName(), user.getProfilePicture());
    }
}
