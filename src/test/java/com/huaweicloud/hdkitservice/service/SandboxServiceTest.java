package com.huaweicloud.hdkitservice.service;

import com.huaweicloud.hdkitservice.config.HdkitConfig;
import com.huaweicloud.hdkitservice.model.CheckUserResponse;
import com.huaweicloud.hdkitservice.model.ConnectRequest;
import com.huaweicloud.hdkitservice.model.ConnectResponse;
import com.huaweicloud.hdkitservice.model.CredentialsRequest;
import com.huaweicloud.hdkitservice.model.CredentialsResponse;
import com.huaweicloud.hdkitservice.model.SignAgreementResponse;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SandboxServiceTest {

    private DevStationClient devStation;
    private SandboxService service;

    @BeforeEach
    void setUp() {
        devStation = org.mockito.Mockito.mock(DevStationClient.class);
        HdkitConfig config = new HdkitConfig();
        config.setEndpoint("https://devstation.myhuaweicloud.com");
        config.setSource("CLI");
        config.setTemplateId("tpl");
        config.setFlavorId("flv");
        config.setPollIntervalMs(1);
        config.setConnectTimeout(1000);
        config.setReleaseTimeout(1000);
        config.setMaxConcurrent(5);
        service = new SandboxService(devStation, config);
    }

    // ── connect: 新建实例 ──

    @Test
    void connectCreatesNewInstanceWhenNoneExists() {
        when(devStation.list("", "AK", "SK")).thenReturn(List.of());
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
        assertEquals("dev1", resp.sessionId()); // session_id 等价 dev_stage_id
        assertEquals("wss://example/1&source=-2074327356", resp.connectionAddress());
        assertEquals("connected", resp.status());
        verify(devStation).autoConfig("dev1", true, "AK", "SK");
    }

    @Test
    void connectPicksConnectedConnectionFromList() {
        when(devStation.list("", "AK", "SK")).thenReturn(List.of());
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
    void connectThrowsConflictWhenAccountEnvCountAtLimit() {
        when(devStation.list("", "AK", "SK")).thenReturn(List.of(
                new DevStationClient.Devenv("m1", "manual1", "cde.0002"),
                new DevStationClient.Devenv("m2", "manual2", "cde.0004"),
                new DevStationClient.Devenv("m3", "manual3", "cde.0004"),
                new DevStationClient.Devenv("m4", "manual4", "cde.0004"),
                new DevStationClient.Devenv("m5", "manual5", "cde.0004")));

        SandboxService.HdkitException ex = assertThrows(SandboxService.HdkitException.class,
                () -> service.connect(new ConnectRequest(null, null, null, null, Map.of(), Map.of()), "AK", "SK"));
        assertEquals("HDKIT_CONFLICT", ex.code());
        verify(devStation, never()).create(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void connectCreatesWhenAccountEnvCountBelowLimit() {
        when(devStation.list("", "AK", "SK")).thenReturn(List.of(
                new DevStationClient.Devenv("m1", "manual1", "cde.0002"),
                new DevStationClient.Devenv("m2", "manual2", "cde.0004")));
        when(devStation.create(any(), any(), any(), any(), any(), any(), eq("AK"), eq("SK"))).thenReturn("dev1");
        when(devStation.statusOf("dev1", "AK", "SK")).thenReturn("cde.0004", "cde.0004", "cde.0002");
        when(devStation.connections("dev1", "CLI", "AK", "SK"))
                .thenReturn(new DevStationClient.Connections(100L,
                        List.of(new DevStationClient.Conn(100L, "CONNECTED"))));
        when(devStation.address("dev1", 100L, "AK", "SK"))
                .thenReturn(new DevStationClient.ConnectionAddress("wss://x", "-1"));

        ConnectResponse resp = service.connect(
                new ConnectRequest(null, null, null, null, Map.of(), Map.of()), "AK", "SK");

        assertEquals("dev1", resp.devStageId());
        verify(devStation).create(any(), any(), any(), any(), any(), any(), eq("AK"), eq("SK"));
    }

    @Test
    void connectFailureAfterCreateRollsBackRelease() {
        when(devStation.list("", "AK", "SK")).thenReturn(List.of());
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
        assertEquals("dev1", resp.sessionId());
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
        when(devStation.statusOf("dev1", "AK", "SK")).thenReturn("cde.0002");
        when(devStation.autoConfig("dev1", true, "AK", "SK")).thenReturn("2026-08-14T04:39:54Z");

        CredentialsResponse resp = service.credentials(new CredentialsRequest(null, "dev1", true), "AK", "SK");

        assertEquals("2026-08-14T04:39:54Z", resp.expiresAt());
        assertEquals("dev1", resp.sessionId()); // session_id 等价 dev_stage_id
    }

    @Test
    void credentialsTreatsSessionIdAsDevStageId() {
        when(devStation.statusOf("s1", "AK", "SK")).thenReturn("cde.0002");
        when(devStation.autoConfig("s1", true, "AK", "SK")).thenReturn("expiry");

        CredentialsResponse resp = service.credentials(new CredentialsRequest("s1", null, null), "AK", "SK");

        assertEquals("expiry", resp.expiresAt());
        assertEquals("s1", resp.sessionId());
    }

    @Test
    void credentialsEnvGoneThrowsNotFound() {
        when(devStation.statusOf("dev1", "AK", "SK")).thenReturn(null);

        SandboxService.HdkitException ex = assertThrows(SandboxService.HdkitException.class,
                () -> service.credentials(new CredentialsRequest(null, "dev1", true), "AK", "SK"));
        assertEquals("HDKIT_SANDBOX_NOT_FOUND", ex.code());
        verify(devStation, never()).autoConfig(any(), anyBoolean(), any(), any());
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

    // ── check-user / sign-agreement ──

    @Test
    void checkUserHappyPathAlwaysQueriesUpstream() {
        when(devStation.realNameStatus("AK", "SK")).thenReturn("2");
        when(devStation.agreements("AK", "SK"))
                .thenReturn(List.of(new DevStationClient.Agreement("90102", "cn", "zh_cn", 1, 2025062315L)));

        CheckUserResponse resp = service.checkUser("AK", "SK");

        assertTrue(resp.realnameVerified());
        assertTrue(resp.agreementSigned());
        // 不再有缓存，每次都必须实时查询上游
        verify(devStation).realNameStatus("AK", "SK");
        verify(devStation).agreements("AK", "SK");
    }

    @Test
    void checkUserNotRealnameThrows() {
        when(devStation.realNameStatus("AK", "SK")).thenReturn("0");
        when(devStation.agreements("AK", "SK"))
                .thenReturn(List.of(new DevStationClient.Agreement("90102", "cn", "zh_cn", 1, 2025062315L)));

        SandboxService.HdkitException ex = assertThrows(SandboxService.HdkitException.class,
                () -> service.checkUser("AK", "SK"));
        assertEquals("HDKIT_NOT_REALNAME", ex.code());
    }

    @Test
    void checkUserNotAgreementThrows() {
        when(devStation.realNameStatus("AK", "SK")).thenReturn("2");
        when(devStation.agreements("AK", "SK"))
                .thenReturn(List.of(new DevStationClient.Agreement("90102", "cn", "zh_cn", 3, 2025062315L)));

        SandboxService.HdkitException ex = assertThrows(SandboxService.HdkitException.class,
                () -> service.checkUser("AK", "SK"));
        assertEquals("HDKIT_NOT_AGREEMENT", ex.code());
    }

    @Test
    void signAgreementSignsOnlyUnsigned() {
        when(devStation.agreements("AK", "SK")).thenReturn(List.of(
                new DevStationClient.Agreement("90102", "cn", "zh_cn", 3, 2025062315L),
                new DevStationClient.Agreement("90142", "cn", "zh_cn", 1, 2026060305L)));

        SignAgreementResponse resp = service.signAgreement("AK", "SK");

        assertTrue(resp.signed());
        assertEquals(1, resp.signedCount());
        verify(devStation).signAgreements(argThat(l -> l.size() == 1
                && "90102".equals(l.get(0).agrType())), eq("AK"), eq("SK"));
    }
}
