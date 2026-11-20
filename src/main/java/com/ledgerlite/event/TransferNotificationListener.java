package com.ledgerlite.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerlite.domain.NotificationLog;
import com.ledgerlite.repository.NotificationLogRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Component
public class TransferNotificationListener {

    private final NotificationLogRepository notificationLogRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public TransferNotificationListener(NotificationLogRepository notificationLogRepository,
                                          ObjectMapper objectMapper,
                                          Clock clock) {
        this.notificationLogRepository = notificationLogRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTransferCompleted(TransferCompletedEvent event) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            payload = "{\"transferId\":\"" + event.transferId() + "\"}";
        }
        NotificationLog log = new NotificationLog(
                UUID.randomUUID(),
                "TransferCompleted",
                payload,
                Instant.now(clock)
        );
        notificationLogRepository.save(log);
    }
}