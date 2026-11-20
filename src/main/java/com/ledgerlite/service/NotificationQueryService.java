package com.ledgerlite.service;

import com.ledgerlite.domain.NotificationLog;
import com.ledgerlite.dto.NotificationResponse;
import com.ledgerlite.repository.NotificationLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationQueryService {

    private final NotificationLogRepository repository;

    public NotificationQueryService(NotificationLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> list(Pageable pageable) {
        return repository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toResponse);
    }

    private NotificationResponse toResponse(NotificationLog log) {
        return new NotificationResponse(log.getId(), log.getEventType(), log.getPayload(), log.getCreatedAt());
    }
}