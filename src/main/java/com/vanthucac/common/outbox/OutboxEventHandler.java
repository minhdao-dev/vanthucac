package com.vanthucac.common.outbox;

public interface OutboxEventHandler {

    boolean supports(String eventType);

    void handle(OutboxEvent event);
}