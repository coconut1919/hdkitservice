package com.huaweicloud.hdkitservice.service;

import com.huaweicloud.hdkitservice.config.HdkitConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IncentiveServiceTest {

    private IncentiveService service;
    private IncentiveClient client;
    private HdkitConfig config;

    @BeforeEach
    void setUp() {
        client = mock(IncentiveClient.class);
        config = new HdkitConfig();
        config.setIncentiveFaceAmount(100);
        config.setDeployEnv("production");
        service = new IncentiveService(client, config);
    }

    // ── Production mode: resolves domainId from IAM ──

    @Test
    void voucherStatusProduction() {
        when(client.resolveDomainIdFromIam("AK", "SK")).thenReturn("domain123");
        when(client.checkCouponIssued("domain123"))
                .thenReturn(new IncentiveClient.CheckResult(false, false, null));

        var result = service.voucherStatus("AK", "SK", null);
        assertFalse(result.claimed());
        assertEquals("未领取", result.message());
    }

    @Test
    void voucherClaimProduction() {
        when(client.resolveDomainIdFromIam("AK", "SK")).thenReturn("domain123");
        when(client.checkCouponIssued("domain123"))
                .thenReturn(new IncentiveClient.CheckResult(false, false, null));
        when(client.issueCoupon("domain123"))
                .thenReturn(new IncentiveClient.IssueResult(true, "c123", null, null));

        var result = service.voucherClaim("AK", "SK", null);
        assertTrue(result.claimed());
        assertEquals("c123", result.voucherId());
    }

    @Test
    void voucherClaimProductionIgnoresProvidedDomainId() {
        when(client.resolveDomainIdFromIam("AK", "SK")).thenReturn("iam-domain");
        when(client.checkCouponIssued("iam-domain"))
                .thenReturn(new IncentiveClient.CheckResult(true, false, null));

        // Even if domain_id is provided, production uses IAM
        var result = service.voucherClaim("AK", "SK", "user-provided-domain");
        assertTrue(result.claimed());
        assertEquals("已领取过", result.message());
        verify(client).checkCouponIssued("iam-domain");
        verify(client, never()).checkCouponIssued("user-provided-domain");
    }

    @Test
    void voucherClaimAlreadyClaimed() {
        when(client.resolveDomainIdFromIam("AK", "SK")).thenReturn("domain123");
        when(client.checkCouponIssued("domain123"))
                .thenReturn(new IncentiveClient.CheckResult(true, false, null));

        var result = service.voucherClaim("AK", "SK", null);
        assertTrue(result.claimed());
        assertEquals("已领取过", result.message());
        verify(client, never()).issueCoupon(anyString());
    }

    @Test
    void voucherClaimCheckErrorThrows() {
        when(client.resolveDomainIdFromIam("AK", "SK")).thenReturn("domain123");
        when(client.checkCouponIssued("domain123"))
                .thenReturn(new IncentiveClient.CheckResult(false, true, "timeout"));

        var ex = assertThrows(SandboxService.HdkitException.class,
                () -> service.voucherClaim("AK", "SK", null));
        assertEquals("HDKIT_INCENTIVE_ERROR", ex.code());
    }

    @Test
    void voucherClaimIssueHandlesAlreadyClaimedErrorCode() {
        when(client.resolveDomainIdFromIam("AK", "SK")).thenReturn("domain123");
        when(client.checkCouponIssued("domain123"))
                .thenReturn(new IncentiveClient.CheckResult(false, false, null));
        when(client.issueCoupon("domain123"))
                .thenReturn(new IncentiveClient.IssueResult(false, null, "already", "HD.60620016"));

        var result = service.voucherClaim("AK", "SK", null);
        assertTrue(result.claimed());
        assertEquals("已领取过", result.message());
    }

    @Test
    void voucherClaimIssueHandlesQuotaExhausted() {
        when(client.resolveDomainIdFromIam("AK", "SK")).thenReturn("domain123");
        when(client.checkCouponIssued("domain123"))
                .thenReturn(new IncentiveClient.CheckResult(false, false, null));
        when(client.issueCoupon("domain123"))
                .thenReturn(new IncentiveClient.IssueResult(false, null, "quota", "HD.60630042"));

        var ex = assertThrows(SandboxService.HdkitException.class,
                () -> service.voucherClaim("AK", "SK", null));
        assertEquals("HDKIT_INCENTIVE_ERROR", ex.code());
        assertTrue(ex.getMessage().contains("已用完"));
    }

    @Test
    void voucherClaimIssueHandlesRealNameRequired() {
        when(client.resolveDomainIdFromIam("AK", "SK")).thenReturn("domain123");
        when(client.checkCouponIssued("domain123"))
                .thenReturn(new IncentiveClient.CheckResult(false, false, null));
        when(client.issueCoupon("domain123"))
                .thenReturn(new IncentiveClient.IssueResult(false, null, "unverified", "HD.60630022"));

        var ex = assertThrows(SandboxService.HdkitException.class,
                () -> service.voucherClaim("AK", "SK", null));
        assertEquals("HDKIT_INCENTIVE_ERROR", ex.code());
        assertTrue(ex.getMessage().contains("实名认证"));
    }

    // ── Test mode ──

    @Test
    void voucherStatusTestUsesProvidedDomainId() {
        config.setDeployEnv("test");

        when(client.checkCouponIssued("test-domain"))
                .thenReturn(new IncentiveClient.CheckResult(false, false, null));

        var result = service.voucherStatus("AK", "SK", "test-domain");
        assertFalse(result.claimed());
        assertEquals("未领取", result.message());
    }

    @Test
    void voucherStatusTestThrowsWithoutDomainId() {
        config.setDeployEnv("test");

        var ex = assertThrows(SandboxService.HdkitException.class,
                () -> service.voucherStatus("AK", "SK", null));
        assertEquals("HDKIT_INVALID_REQUEST", ex.code());
        assertEquals("测试环境需提供 domain_id", ex.getMessage());
    }

    @Test
    void voucherClaimTestThrowsWithoutDomainId() {
        config.setDeployEnv("test");

        var ex = assertThrows(SandboxService.HdkitException.class,
                () -> service.voucherClaim("AK", "SK", null));
        assertEquals("HDKIT_INVALID_REQUEST", ex.code());
    }
}