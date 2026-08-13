package com.huaweicloud.hdkitservice.service;

import com.huaweicloud.hdkitservice.config.HdkitConfig;
import com.huaweicloud.hdkitservice.model.ConnectRequest;
import com.huaweicloud.hdkitservice.model.ConnectResponse;
import com.huaweicloud.hdkitservice.model.CredentialsRequest;
import com.huaweicloud.hdkitservice.model.CredentialsResponse;
import com.huaweicloud.hdkitservice.model.ReleaseRequest;
import com.huaweicloud.hdkitservice.model.ReleaseResponse;
import com.huaweicloud.hdkitservice.model.SandboxSession;
import com.huaweicloud.hdkitservice.store.SessionStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SandboxServiceTest {

    private DevStationClient devStation;
    private SessionStore store;
    private SandboxService service;

    @BeforeEach
    void setUp() {
        devStation = org.mockito.Mockito.mock(DevStationClient.class);
        store = org.mockito.Mockito.mock(SessionStore.class);
        HdkitConfig config = new HdkitConfig();
        config.setEndpoint("https://devstation.myhuaweicloud.com");
        config.setSource("CLI");
        config.setTemplateId("tpl");
        config.setFlavorId("flv");
        config.setPollIntervalMs(1);
        config.setConnectTimeout(1000);
        config.setReleaseTimeout(1000);
        config.setMaxConcurrent(5);
        service = new SandboxService(devStation, store, config);
    }

    // ── connect: 新建实例 ──

    @Test
    void connectCreatesNewInstanceWhenNoneExists() {
        when(devStation.list("", "AK", "SK")).thenReturn(List.of());
        when(store.countActive()).thenReturn(0L);
        when(devStation.create(any(), eq("tpl"), eq("flv"), any(), any(), any(), eq("AK"), eq("SK")))
                .thenReturn("dev1");
        when(devStation.statusOf("dev1", "AK", "SK")).thenReturn("cde.0004", "cde.0004", "cde.0002");
        when(devStation.connections("dev1", "CLI", "AK", "SK"))
                .thenReturn(new DevStationClient.Connections(100L,
                        List.of(new DevStationClient.Conn(100L, "CONNECTED"))));
        when(devStation.address("dev1", 100L, "AK", "SK"))
                .thenReturn(new DevStationClient.ConnectionAddress("wss://example/1", "-2074327356"));

        ConnectResponse resp = service.connect(
                new ConnectRequest(null, null, null, null, Map.of(), Map.of()), "AK", "SK");

        assertEquals("dev1", resp.devStageId());
        assertEquals("wss://example/1&source=-2074327356", resp.connectionAddress());
        assertNotNull(resp.sessionId());
        assertEquals("connected", resp.status());
        verify(devStation).autoConfig("dev1", true, "AK", "SK");
        verify(store).save(any());
        verify(store).addActive(any());
    }

    @Test
    void connectPicksConnectedConnectionFromList() {
        when(devStation.list("", "AK", "SK")).thenReturn(List.of());
        when(store.countActive()).thenReturn(0L);
        when(devStation.create(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn("dev1");
        when(devStation.statusOf("dev1", "AK", "SK")).thenReturn("cde.0004", "cde.0004", "cde.0002");
        when(devStation.connections("dev1", "CLI", "AK", "SK"))
                .thenReturn(new DevStationClient.Connections(111L, List.of(
                        new DevStationClient.Conn(111L, "CONNECTING"),
                        new DevStationClient.Conn(222L, "CONNECTED"))));
        when(devStation.address("dev1", 222L, "AK", "SK"))
                .thenReturn(new DevStationClient.ConnectionAddress("wss://example/2", "-2074327356"));

        ConnectResponse resp = service.connect(
                new ConnectRequest(null, null, null, null, Map.of(), Map.of()), "AK", "SK");

        assertEquals("222", resp.connectionId());
        assertEquals("wss://example/2&source=-2074327356", resp.connectionAddress());
    }

    @Test
    void connectUsesRequestTemplateAndFlavorOverride() {
        when(devStation.list("", "AK", "SK")).thenReturn(List.of());
        when(store.countActive()).thenReturn(0L);
        when(devStation.create(any(), eq("customTpl"), eq("customFlv"), any(), any(), any(), eq("AK"), eq("SK")))
                .thenReturn("dev1");
        when(devStation.statusOf("dev1", "AK", "SK")).thenReturn("cde.0004", "cde.0004", "cde.0002");
        when(devStation.connections("dev1", "CLI", "AK", "SK"))
                .thenReturn(new DevStationClient.Connections(100L,
                        List.of(new DevStationClient.Conn(100L, "CONNECTED"))));
        when(devStation.address("dev1", 100L, "AK", "SK"))
                .thenReturn(new DevStationClient.ConnectionAddress("wss://x", "-2074327356"));

        service.connect(new ConnectRequest(null, "customTpl", "customFlv", null, Map.of(), Map.of()), "AK", "SK");

        verify(devStation).create(any(), eq("customTpl"), eq("customFlv"), any(), any(), any(), eq("AK"), eq("SK"));
    }

    @Test
    void connectThrowsConflictAtConcurrencyLimitWhenNoExisting() {
        when(devStation.list("", "AK", "SK")).thenReturn(List.of());
        when(store.countActive()).thenReturn(5L);

        SandboxService.HdkitException ex = assertThrows(SandboxService.HdkitException.class,
                () -> service.connect(new ConnectRequest(null, null, null, null, Map.of(), Map.of()), "AK", "SK"));
        assertEquals("HDKIT_CONFLICT", ex.code());
        verify(devStation, never()).create(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void connectFailureAfterCreateRollsBackRelease() {
        when(devStation.list("", "AK", "SK")).thenReturn(List.of());
        when(store.countActive()).thenReturn(0L);
        when(devStation.create(any(), any(), any(), any(), any(), any(), eq("AK"), eq("SK"))).thenReturn("dev1");
        when(devStation.statusOf("dev1", "AK", "SK")).thenReturn("cde.0004", "cde.0004", "cde.0004", "cde.0004", null);
        doThrow(new RuntimeException("start boom")).when(devStation).start("dev1", "CLI", "AK", "SK");

        SandboxService.HdkitException ex = assertThrows(SandboxService.HdkitException.class,
                () -> service.connect(new ConnectRequest(null, null, null, null, Map.of(), Map.of()), "AK", "SK"));
        assertEquals("HDKIT_CONNECT_FAILED", ex.code());
        verify(devStation).close("dev1", "CLI", "AK", "SK");
        verify(devStation).delete("dev1", "CLI", "AK", "SK");
    }

    // ── connect: 复用已有实例 ──

    @Test
    void connectReusesExistingStoppedInstance() {
        when(devStation.list("", "AK", "SK"))
                .thenReturn(List.of(new DevStationClient.Devenv("dev1", "hcdkabc", "cde.0004")));
        when(devStation.statusOf("dev1", "AK", "SK")).thenReturn("cde.0004", "cde.0002");
        when(devStation.connections("dev1", "CLI", "AK", "SK"))
                .thenReturn(new DevStationClient.Connections(200L,
                        List.of(new DevStationClient.Conn(200L, "CONNECTED"))));
        when(devStation.address("dev1", 200L, "AK", "SK"))
                .thenReturn(new DevStationClient.ConnectionAddress("wss://example/reuse", "-2074327356"));

        ConnectResponse resp = service.connect(
                new ConnectRequest(null, null, null, null, Map.of(), Map.of()), "AK", "SK");

        assertEquals("dev1", resp.devStageId());
        assertEquals("wss://example/reuse&source=-2074327356", resp.connectionAddress());
        verify(devStation, never()).create(any(), any(), any(), any(), any(), any(), any(), any());
        verify(devStation).start("dev1", "CLI", "AK", "SK");
        verify(devStation).autoConfig("dev1", true, "AK", "SK");
    }

    @Test
    void connectReusesRunningInstanceWithoutStart() {
        when(devStation.list("", "AK", "SK"))
                .thenReturn(List.of(new DevStationClient.Devenv("dev1", "hcdkabc", "cde.0002")));
        when(devStation.statusOf("dev1", "AK", "SK")).thenReturn("cde.0002");
        when(devStation.connections("dev1", "CLI", "AK", "SK"))
                .thenReturn(new DevStationClient.Connections(200L,
                        List.of(new DevStationClient.Conn(200L, "CONNECTED"))));
        when(devStation.address("dev1", 200L, "AK", "SK"))
                .thenReturn(new DevStationClient.ConnectionAddress("wss://example/reuse", "-2074327356"));

        ConnectResponse resp = service.connect(
                new ConnectRequest(null, null, null, null, Map.of(), Map.of()), "AK", "SK");

        assertEquals("dev1", resp.devStageId());
        verify(devStation, never()).start(any(), any(), any(), any());
        verify(devStation).autoConfig("dev1", true, "AK", "SK");
    }

    @Test
    void connectReuseFailureDoesNotReleaseExisting() {
        when(devStation.list("", "AK", "SK"))
                .thenReturn(List.of(new DevStationClient.Devenv("dev1", "hcdkabc", "cde.0002")));
        when(devStation.statusOf("dev1", "AK", "SK")).thenReturn("cde.0002");
        doThrow(new DevStationClient.DevStationException("auto-config failed", null))
                .when(devStation).autoConfig("dev1", true, "AK", "SK");

        SandboxService.HdkitException ex = assertThrows(SandboxService.HdkitException.class,
                () -> service.connect(new ConnectRequest(null, null, null, null, Map.of(), Map.of()), "AK", "SK"));
        assertEquals("HDKIT_CONNECT_FAILED", ex.code());
        // 复用实例失败不应释放用户已有沙箱
        verify(devStation, never()).close(any(), any(), any(), any());
        verify(devStation, never()).delete(any(), any(), any(), any());
    }

    // ── credentials ──

    @Test
    void credentialsWithDevStageIdReturnsExpiryAndSession() {
        when(store.findByDevStageId("dev1")).thenReturn(null);
        when(devStation.statusOf("dev1", "AK", "SK")).thenReturn("cde.0002");
        when(devStation.autoConfig("dev1", true, "AK", "SK")).thenReturn("2026-08-14T04:39:54Z");

        CredentialsResponse resp = service.credentials(new CredentialsRequest(null, "dev1", true), "AK", "SK");

        assertEquals("2026-08-14T04:39:54Z", resp.expiresAt());
        assertNotNull(resp.sessionId());
        verify(store).save(any());
    }

    @Test
    void credentialsResolvesDevStageIdFromSession() {
        SandboxSession s = new SandboxSession("s1", "n1", "dev1", "1", "wss://x", "connected", 1L, 2L);
        when(store.get("s1")).thenReturn(s);
        when(store.findByDevStageId("dev1")).thenReturn("s1");
        when(devStation.statusOf("dev1", "AK", "SK")).thenReturn("cde.0002");
        when(devStation.autoConfig("dev1", true, "AK", "SK")).thenReturn("expiry");

        CredentialsResponse resp = service.credentials(new CredentialsRequest("s1", null, null), "AK", "SK");

        assertEquals("expiry", resp.expiresAt());
        assertEquals("s1", resp.sessionId());
    }

    @Test
    void credentialsNotRunningThrows() {
        when(devStation.statusOf("dev1", "AK", "SK")).thenReturn("cde.0004");

        SandboxService.HdkitException ex = assertThrows(SandboxService.HdkitException.class,
                () -> service.credentials(new CredentialsRequest(null, "dev1", true), "AK", "SK"));
        assertEquals("HDKIT_NOT_RUNNING", ex.code());
        verify(devStation, never()).autoConfig(any(), anyBoolean(), any(), any());
    }

    @Test
    void credentialsMissingIdsThrows() {
        SandboxService.HdkitException ex = assertThrows(SandboxService.HdkitException.class,
                () -> service.credentials(new CredentialsRequest(null, null, true), "AK", "SK"));
        assertEquals("HDKIT_INVALID_REQUEST", ex.code());
    }

    // ── release ──

    @Test
    void releaseHappyPath() {
        when(devStation.statusOf("dev1", "AK", "SK")).thenReturn("cde.0004", "cde.0004", null);
        when(store.findByDevStageId("dev1")).thenReturn("s1");

        ReleaseResponse resp = service.release(new ReleaseRequest(null, "dev1"), "AK", "SK");

        assertTrue(resp.released());
        assertEquals("dev1", resp.devStageId());
        verify(devStation).close("dev1", "CLI", "AK", "SK");
        verify(devStation).delete("dev1", "CLI", "AK", "SK");
        verify(store).delete("s1");
    }

    @Test
    void releaseIsIdempotentWhenEnvAlreadyGone() {
        when(devStation.statusOf("dev1", "AK", "SK")).thenReturn(null);
        when(store.findByDevStageId("dev1")).thenReturn(null);

        ReleaseResponse resp = service.release(new ReleaseRequest(null, "dev1"), "AK", "SK");

        assertTrue(resp.released());
        verify(devStation, never()).close(any(), any(), any(), any());
        verify(devStation, never()).delete(any(), any(), any(), any());
    }

    @Test
    void releaseFailureMarksReleaseFailed() {
        when(devStation.statusOf("dev1", "AK", "SK")).thenReturn("cde.0002");
        doThrow(new DevStationClient.DevStationException("close failed", null))
                .when(devStation).close("dev1", "CLI", "AK", "SK");
        when(store.findByDevStageId("dev1")).thenReturn("s1");
        when(store.get("s1")).thenReturn(new SandboxSession("s1", "n1", "dev1", "1", "wss://x", "connected", 1L, 2L));

        SandboxService.HdkitException ex = assertThrows(SandboxService.HdkitException.class,
                () -> service.release(new ReleaseRequest(null, "dev1"), "AK", "SK"));
        assertEquals("HDKIT_RELEASE_FAILED", ex.code());
        verify(store).save(argThat(s -> "release_failed".equals(s.status())));
    }

    @Test
    void releaseMissingIdsThrows() {
        SandboxService.HdkitException ex = assertThrows(SandboxService.HdkitException.class,
                () -> service.release(new ReleaseRequest(null, null), "AK", "SK"));
        assertEquals("HDKIT_INVALID_REQUEST", ex.code());
    }
}
