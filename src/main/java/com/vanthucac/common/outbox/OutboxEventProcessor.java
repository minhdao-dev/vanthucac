package com.vanthucac.common.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class OutboxEventProcessor {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventProcessor.class);

    private final OutboxEventRepository outboxEventRepository;
    private final List<OutboxEventHandler> handlers;

    public OutboxEventProcessor(
            OutboxEventRepository outboxEventRepository,
            List<OutboxEventHandler> handlers
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.handlers = handlers;
    }

    @Scheduled(fixedDelayString = "${app.outbox.processing-delay-ms:5000}")
    @Transactional
    public void processPendingEvents() {
        var events = outboxEventRepository.findTop50ByStatusAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
                OutboxEvent.OutboxStatus.PENDING,
                Instant.now()
        );

        for (var event : events) {
            processEvent(event);
        }
    }

    private void processEvent(OutboxEvent event) {
        if (!event.canProcess()) {
            return;
        }

        try {
            event.markProcessing();
            findHandler(event).handle(event);
            event.markProcessed();
        } catch (Exception ex) {
            var nextRetryAt = Instant.now().plus(calculateRetryDelay(event.getRetryCount()));
            event.markFailed(ex.getMessage(), nextRetryAt);
            log.error("Failed to process outbox event {} — type: {}", event.getId(), event.getEventType(), ex);
        }
    }

    private OutboxEventHandler findHandler(OutboxEvent event) {
        return handlers.stream()
                .filter(handler -> handler.supports(event.getEventType()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No outbox handler found for event type " + event.getEventType()
                ));
    }

    private Duration calculateRetryDelay(int retryCount) {
        return switch (retryCount) {
            case 0 -> Duration.ofSeconds(10);
            case 1 -> Duration.ofSeconds(30);
            case 2 -> Duration.ofMinutes(1);
            case 3 -> Duration.ofMinutes(5);
            default -> Duration.ofMinutes(15);
        };
    }
}