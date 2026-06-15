package com.wfhwfo.attendance.outbox.repository;

import com.wfhwfo.attendance.outbox.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query(value = """
            SELECT * FROM outbox_events
            WHERE status = 'PENDING'
            ORDER BY created_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> findPendingEvents(@Param("limit") int limit);

    @Modifying
    @Query(value = """
            UPDATE outbox_events
            SET status = 'PROCESSING', updated_at = NOW()
            WHERE id = :id AND status = 'PENDING'
            """, nativeQuery = true)
    int claimEvent(@Param("id") Long id);

    @Modifying
    @Query(value = """
            UPDATE outbox_events
            SET status = 'PROCESSED', processed_at = NOW(), error_message = NULL, updated_at = NOW()
            WHERE id = :id AND status = 'PROCESSING'
            """, nativeQuery = true)
    int markProcessedIfProcessing(@Param("id") Long id);

    @Modifying
    @Query(value = """
            UPDATE outbox_events
            SET status = CASE
                    WHEN retry_count + 1 >= max_retries THEN 'FAILED'
                    ELSE 'PENDING'
                END,
                retry_count = retry_count + 1,
                error_message = :errorMessage,
                updated_at = NOW()
            WHERE id = :id AND status = 'PROCESSING'
            """, nativeQuery = true)
    int markFailedIfProcessing(@Param("id") Long id, @Param("errorMessage") String errorMessage);

    @Modifying
    @Query(value = """
            UPDATE outbox_events
            SET status = 'PENDING', updated_at = NOW()
            WHERE status = 'PROCESSING'
              AND updated_at < NOW() - INTERVAL '5 minutes'
            """, nativeQuery = true)
    int resetStaleProcessingEvents();
}
