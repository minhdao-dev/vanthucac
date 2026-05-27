package com.vanthucac.common.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingOutboxEventHandler implements OutboxEventHandler {

    private static final Logger log = LoggerFactory.getLogger(LoggingOutboxEventHandler.class);

    @Override
    public boolean supports(String eventType) {
        return true;
    }

    @Override
    public void handle(OutboxEvent event) {
        log.info(
                "Outbox event handled by fallback logger — id: {}, type: {}, aggregate: {}#{}, payload: {}",
                event.getId(),
                event.getEventType(),
                event.getAggregateType(),
                event.getAggregateId(),
                event.getPayload()
        );
    }
}