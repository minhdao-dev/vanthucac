package com.vanthucac.common.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findTop50ByStatusAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
            OutboxEvent.OutboxStatus status,
            Instant now
    );
}