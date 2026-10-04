package com.photobuddy.repository;

import com.photobuddy.dto.post.PostFeedResponse;
import com.photobuddy.entity.Post;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {
    long countByUserId(Long userId);

    @Query(value = "select new com.photobuddy.dto.post.PostFeedResponse(p.id, " +
            "new com.photobuddy.dto.post.PostUserResponse(u.id, u.username, u.firstName, u.lastName, u.profilePicture), " +
            "p.imageUrl, p.caption, p.style, " +
            "(select count(l.id) from PostLike l where l.post = p), " +
            "(select count(c.id) from Comment c where c.post = p), p.createdAt, " +
            "case when exists (select l2.id from PostLike l2 where l2.post = p and l2.user.id = :viewerId) " +
            "then true else false end) " +
            "from Post p join p.user u order by p.createdAt desc",
            countQuery = "select count(p) from Post p")
    Page<PostFeedResponse> findFeed(@Param("viewerId") Long viewerId, Pageable pageable);

    @Query("select new com.photobuddy.dto.post.PostFeedResponse(p.id, " +
            "new com.photobuddy.dto.post.PostUserResponse(u.id, u.username, u.firstName, u.lastName, u.profilePicture), " +
            "p.imageUrl, p.caption, p.style, (select count(l.id) from PostLike l where l.post = p), " +
            "(select count(c.id) from Comment c where c.post = p), p.createdAt, " +
            "case when exists (select l2.id from PostLike l2 where l2.post = p and l2.user.id = :viewerId) " +
            "then true else false end) from Post p join p.user u where p.id = :postId")
    Optional<PostFeedResponse> findPostView(@Param("postId") Long postId, @Param("viewerId") Long viewerId);
}
