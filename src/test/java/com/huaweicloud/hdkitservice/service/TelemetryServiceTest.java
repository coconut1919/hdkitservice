package com.huaweicloud.hdkitservice.service;

import com.huaweicloud.hdkitservice.model.TelemetryEvent;
import com.huaweicloud.hdkitservice.model.TelemetryEventDto;
import com.huaweicloud.hdkitservice.repository.TelemetryEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TelemetryServiceTest {

    private TelemetryEventRepository repository;
    private TelemetryService telemetryService;

    @BeforeEach
    void setUp() {
        repository = mock(TelemetryEventRepository.class);
        telemetryService = new TelemetryService(repository);
    }

    @Test
    void saveBatchEmptyListReturnsZero() {
        int received = telemetryService.saveBatch(Collections.emptyList());
        assertEquals(0, received);
    }

    @Test
    void saveBatchNullListReturnsZero() {
        int received = telemetryService.saveBatch(null);
        assertEquals(0, received);
    }

    @Test
    void saveBatchSingleEvent() {
        TelemetryEventDto dto = new TelemetryEventDto("key1", "value1", "mcp",
                "inst1", "hash1", "1.0", "vscode", "1.2.3", "win", "10");
        List<TelemetryEventDto> dtos = List.of(dto);

        int received = telemetryService.saveBatch(dtos);
        assertEquals(1, received);
        verify(repository, times(1)).saveAll(anyList());
    }

    @Test
    void saveBatchMultipleEvents() {
        TelemetryEventDto dto1 = new TelemetryEventDto("key1", "value1", "mcp",
                "inst1", "hash1", "1.0", "vscode", "1.2.3", "win", "10");
        TelemetryEventDto dto2 = new TelemetryEventDto("key2", "value2", "cli",
                "inst2", "hash2", "2.0", "cursor", "2.0.0", "mac", "14");
        List<TelemetryEventDto> dtos = Arrays.asList(dto1, dto2);

        int received = telemetryService.saveBatch(dtos);
        assertEquals(2, received);
        verify(repository, times(1)).saveAll(anyList());
    }

    @Test
    void saveBatchExceedsMaxSize() {
        TelemetryEventDto dto = new TelemetryEventDto("key1", "value1", null,
                "inst1", null, null, null, null, null, null);
        List<TelemetryEventDto> dtos = Collections.nCopies(101, dto);

        SandboxService.HdkitException ex = assertThrows(SandboxService.HdkitException.class,
                () -> telemetryService.saveBatch(dtos));
        assertEquals("HDKIT_INVALID_REQUEST", ex.code());
    }

    @Test
    void saveBatchMissingInstallId() {
        TelemetryEventDto dto = new TelemetryEventDto("key1", "value1", "mcp",
                null, "hash1", "1.0", "vscode", "1.2.3", "win", "10");

        SandboxService.HdkitException ex = assertThrows(SandboxService.HdkitException.class,
                () -> telemetryService.saveBatch(List.of(dto)));
        assertEquals("HDKIT_INVALID_REQUEST", ex.code());
        assertTrue(ex.getMessage().contains("installId"));
    }

    @Test
    void saveBatchBlankInstallId() {
        TelemetryEventDto dto = new TelemetryEventDto("key1", "value1", "mcp",
                "  ", "hash1", "1.0", "vscode", "1.2.3", "win", "10");

        SandboxService.HdkitException ex = assertThrows(SandboxService.HdkitException.class,
                () -> telemetryService.saveBatch(List.of(dto)));
        assertEquals("HDKIT_INVALID_REQUEST", ex.code());
    }

    @Test
    void saveBatchMissingEventKey() {
        TelemetryEventDto dto = new TelemetryEventDto(null, "value1", "mcp",
                "inst1", "hash1", "1.0", "vscode", "1.2.3", "win", "10");

        SandboxService.HdkitException ex = assertThrows(SandboxService.HdkitException.class,
                () -> telemetryService.saveBatch(List.of(dto)));
        assertTrue(ex.getMessage().contains("eventKey"));
    }

    @Test
    void saveBatchMissingEventValue() {
        TelemetryEventDto dto = new TelemetryEventDto("key1", null, "mcp",
                "inst1", "hash1", "1.0", "vscode", "1.2.3", "win", "10");

        SandboxService.HdkitException ex = assertThrows(SandboxService.HdkitException.class,
                () -> telemetryService.saveBatch(List.of(dto)));
        assertTrue(ex.getMessage().contains("eventValue"));
    }

    @Test
    void saveBatchFallsBackToIndividualOnDuplicate() {
        TelemetryEventDto dto1 = new TelemetryEventDto("key1", "value1", "mcp",
                "inst1", "hash1", "1.0", "vscode", "1.2.3", "win", "10");
        TelemetryEventDto dto2 = new TelemetryEventDto("key2", "value2", "cli",
                "inst2", "hash2", "2.0", "cursor", "2.0.0", "mac", "14");

        doThrow(new DataIntegrityViolationException("duplicate"))
                .when(repository).saveAll(anyList());

        int received = telemetryService.saveBatch(List.of(dto1, dto2));
        assertEquals(2, received);
        verify(repository, times(1)).saveAll(anyList());
        verify(repository, times(2)).save(any(TelemetryEvent.class));
    }
}