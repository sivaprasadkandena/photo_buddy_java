package com.photobuddy.repository;

import com.photobuddy.entity.BuddyRequest;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BuddyRequestRepository extends JpaRepository<BuddyRequest, Long> {
    Optional<BuddyRequest> findByPairLowIdAndPairHighId(Long pairLowId, Long pairHighId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"sender", "receiver", "pairLow", "pairHigh"})
    @Query("select request from BuddyRequest request where request.id = :id")
    Optional<BuddyRequest> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"sender", "receiver"})
    List<BuddyRequest> findAllByReceiverIdOrderByUpdatedAtDesc(Long receiverId);

    @EntityGraph(attributePaths = {"sender", "receiver"})
    List<BuddyRequest> findAllBySenderIdOrderByUpdatedAtDesc(Long senderId);
}
