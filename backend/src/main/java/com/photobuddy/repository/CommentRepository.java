package com.photobuddy.repository;

import com.photobuddy.dto.post.CommentResponse;
import com.photobuddy.entity.Comment;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    long countByPostId(Long postId);
    Optional<Comment> findByIdAndUserId(Long id, Long userId);
    void deleteByPostId(Long postId);

    @Query(value = "select new com.photobuddy.dto.post.CommentResponse(c.id, " +
            "new com.photobuddy.dto.post.PostUserResponse(u.id, u.username, u.firstName, u.lastName, u.profilePicture), " +
            "c.content, c.createdAt) from Comment c join c.user u where c.post.id = :postId order by c.createdAt asc",
            countQuery = "select count(c) from Comment c where c.post.id = :postId")
    Page<CommentResponse> findForPost(@Param("postId") Long postId, Pageable pageable);
}
