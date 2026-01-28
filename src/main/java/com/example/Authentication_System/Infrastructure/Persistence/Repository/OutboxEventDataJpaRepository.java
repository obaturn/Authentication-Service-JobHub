package com.example.Authentication_System.Infrastructure.Persistence.Repository;

import com.example.Authentication_System.Infrastructure.Persistence.Entity.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventDataJpaRepository extends JpaRepository<OutboxEventEntity, UUID> {

    List<OutboxEventEntity> findByStatusOrderByCreatedAtAsc(String status);

    @Query("SELECT e FROM OutboxEventEntity e WHERE e.status = :status AND e.createdAt < :before AND e.retryCount < :maxRetries")
    List<OutboxEventEntity> findPendingEventsForRetry(
        @Param("status") String status,
        @Param("before") LocalDateTime before,
        @Param("maxRetries") int maxRetries
    );

    long countByStatus(String status);
}