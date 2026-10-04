package com.photobuddy.repository;

import com.photobuddy.entity.ChatRoom;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findByUser1IdAndUser2Id(Long user1Id, Long user2Id);
    @EntityGraph(attributePaths = {"user1", "user2"})
    List<ChatRoom> findAllByUser1IdOrUser2IdOrderByUpdatedAtDesc(Long user1Id, Long user2Id);
    @EntityGraph(attributePaths = {"user1", "user2"})
    Optional<ChatRoom> findWithUsersById(Long id);
}
