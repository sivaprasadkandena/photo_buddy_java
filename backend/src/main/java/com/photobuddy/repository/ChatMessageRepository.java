package com.photobuddy.repository;

import com.photobuddy.dto.chat.ChatMessageResponse;
import com.photobuddy.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    @Query(value = "select new com.photobuddy.dto.chat.ChatMessageResponse(m.id, r.id, " +
            "new com.photobuddy.dto.chat.ChatUserResponse(u.id, u.username, u.firstName, u.lastName, u.profilePicture), " +
            "m.text, m.imageUrl, m.timestamp, m.isRead) from ChatMessage m join m.room r join m.sender u " +
            "where r.id = :roomId order by m.timestamp desc",
            countQuery = "select count(m) from ChatMessage m where m.room.id = :roomId")
    Page<ChatMessageResponse> findHistory(@Param("roomId") Long roomId, Pageable pageable);

    @Modifying
    @Query("update ChatMessage m set m.isRead = true where m.room.id = :roomId and m.sender.id <> :userId and m.isRead = false")
    int markIncomingRead(@Param("roomId") Long roomId, @Param("userId") Long userId);
}
