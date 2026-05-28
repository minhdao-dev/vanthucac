package com.vanthucac.common.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    @Query("SELECT e.id FROM OutboxEvent e WHERE e.status = :status AND e.nextRetryAt <= :now ORDER BY e.createdAt ASC LIMIT 50")
    List<Long> findTop50IdsByStatusAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
            @Param("status") OutboxEvent.OutboxStatus status,
            @Param("now") Instant now
    );
}