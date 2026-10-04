package com.photobuddy.repository;

import com.photobuddy.entity.BuddyMatch;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BuddyMatchRepository extends JpaRepository<BuddyMatch, Long> {
    boolean existsByUser1IdAndUser2Id(Long user1Id, Long user2Id);
    long countByUser1IdOrUser2Id(Long user1Id, Long user2Id);

    @EntityGraph(attributePaths = {"user1", "user2"})
    List<BuddyMatch> findAllByUser1IdOrUser2IdOrderByCreatedAtDesc(Long user1Id, Long user2Id);
}
