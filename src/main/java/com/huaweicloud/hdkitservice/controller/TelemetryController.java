package com.huaweicloud.hdkitservice.controller;

import com.huaweicloud.hdkitservice.model.TelemetryEventDto;
import com.huaweicloud.hdkitservice.service.SandboxService;
import com.huaweicloud.hdkitservice.service.TelemetryService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rest/developer/server/hdkitservice")
public class TelemetryController {

    private final TelemetryService telemetryService;

    public TelemetryController(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @PostMapping(value = "/telemetry/events", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> postEvents(@RequestBody List<TelemetryEventDto> events) {
        if (events == null || events.isEmpty()) {
            return ResponseEntity.ok(Map.of("received", 0));
        }
        for (TelemetryEventDto e : events) {
            if (e.installId() == null || e.installId().isBlank()) {
                throw new SandboxService.HdkitException("HDKIT_INVALID_REQUEST",
                        "installId is required.", null);
            }
        }
        int received = telemetryService.saveBatch(events);
        return ResponseEntity.ok(Map.of("received", received));
    }
}