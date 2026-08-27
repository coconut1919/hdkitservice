package com.huaweicloud.hdkitservice.service;

import com.huaweicloud.hdkitservice.model.TelemetryEvent;
import com.huaweicloud.hdkitservice.model.TelemetryEventDto;
import com.huaweicloud.hdkitservice.repository.TelemetryEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TelemetryService {

    private static final Logger log = LoggerFactory.getLogger(TelemetryService.class);
    private static final int MAX_BATCH_SIZE = 100;

    private final TelemetryEventRepository repository;

    public TelemetryService(TelemetryEventRepository repository) {
        this.repository = repository;
    }

    public int saveBatch(List<TelemetryEventDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return 0;
        }
        if (dtos.size() > MAX_BATCH_SIZE) {
            throw new SandboxService.HdkitException("HDKIT_INVALID_REQUEST",
                    "batch size exceeds max " + MAX_BATCH_SIZE, null);
        }

        for (int i = 0; i < dtos.size(); i++) {
            TelemetryEventDto dto = dtos.get(i);
            if (dto.key() == null || dto.key().isBlank()) {
                throw new SandboxService.HdkitException("HDKIT_INVALID_REQUEST",
                        "eventKey is required at index " + i, null);
            }
            if (dto.key().length() > 128) {
                throw new SandboxService.HdkitException("HDKIT_INVALID_REQUEST",
                        "eventKey exceeds 128 chars at index " + i, null);
            }
            if (dto.value() == null || dto.value().isBlank()) {
                throw new SandboxService.HdkitException("HDKIT_INVALID_REQUEST",
                        "eventValue is required at index " + i, null);
            }
            if (dto.value().length() > 256) {
                throw new SandboxService.HdkitException("HDKIT_INVALID_REQUEST",
                        "eventValue exceeds 256 chars at index " + i, null);
            }
            if (dto.installId() == null || dto.installId().isBlank()) {
                throw new SandboxService.HdkitException("HDKIT_INVALID_REQUEST",
                        "installId is required at index " + i, null);
            }
        }

        long now = System.currentTimeMillis();
        LocalDateTime serverTime = LocalDateTime.now();
        List<TelemetryEvent> events = new ArrayList<>(dtos.size());

        for (TelemetryEventDto dto : dtos) {
            TelemetryEvent e = new TelemetryEvent(
                    UUID.randomUUID().toString(),
                    dto.key(),
                    dto.value(),
                    dto.installId(),
                    dto.userHash(),
                    dto.version(),
                    dto.harness(),
                    dto.agentVersion(),
                    dto.os(),
                    dto.osVersion(),
                    dto.capability(),
                    now,
                    serverTime
            );
            events.add(e);
        }

        try {
            repository.saveAll(events);
            log.info("[telemetry] batch saved {} events", events.size());
            return events.size();
        } catch (DataIntegrityViolationException ex) {
            log.warn("[telemetry] batch save had duplicates, retrying individually");
            int saved = 0;
            for (TelemetryEvent e : events) {
                try {
                    repository.save(e);
                    saved++;
                } catch (DataIntegrityViolationException e2) {
                    log.debug("[telemetry] duplicate event_id {}, skipping", e.getEventId());
                }
            }
            log.info("[telemetry] individual saved {} / {} events", saved, events.size());
            return saved;
        }
    }
}