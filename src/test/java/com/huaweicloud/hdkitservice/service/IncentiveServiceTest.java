package com.huaweicloud.hdkitservice.service;

import com.huaweicloud.hdkitservice.config.HdkitConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    // ── Production mode ──

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
    void voucherClaimProductionSuccess() {
        when(client.resolveDomainIdFromIam("AK", "SK")).thenReturn("domain123");
        when(client.checkCouponIssued("domain123"))
                .thenReturn(new IncentiveClient.CheckResult(false, false, null));
        when(client.issueCoupon("domain123"))
                .thenReturn(new IncentiveClient.IssueResult(true, "c123", null, null));

        var result = service.voucherClaim("AK", "SK", null);
        assertTrue(result.claimed());
        assertEquals("c123", result.voucherId());
        assertEquals("领取成功", result.message());
    }

    @Test
    void voucherClaimProductionIgnoresProvidedDomainId() {
        when(client.resolveDomainIdFromIam("AK", "SK")).thenReturn("iam-domain");
        when(client.checkCouponIssued("iam-domain"))
                .thenReturn(new IncentiveClient.CheckResult(true, false, null));

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
    }

    @Test
    void voucherClaimCheckErrorReturnsGracefulMessage() {
        when(client.resolveDomainIdFromIam("AK", "SK")).thenReturn("domain123");
        when(client.checkCouponIssued("domain123"))
                .thenReturn(new IncentiveClient.CheckResult(false, true, "timeout"));

        var result = service.voucherClaim("AK", "SK", null);
        assertFalse(result.claimed());
        assertEquals("激励服务查询失败，请稍后重试", result.message());
    }

    @Test
    void voucherClaimIssueAlreadyClaimedErrorCode() {
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
    void voucherClaimIssueQuotaExhausted() {
        when(client.resolveDomainIdFromIam("AK", "SK")).thenReturn("domain123");
        when(client.checkCouponIssued("domain123"))
                .thenReturn(new IncentiveClient.CheckResult(false, false, null));
        when(client.issueCoupon("domain123"))
                .thenReturn(new IncentiveClient.IssueResult(false, null, "quota", "HD.60630042"));

        var result = service.voucherClaim("AK", "SK", null);
        assertFalse(result.claimed());
        assertTrue(result.message().contains("已用完"));
    }

    @Test
    void voucherClaimIssueRealNameRequired() {
        when(client.resolveDomainIdFromIam("AK", "SK")).thenReturn("domain123");
        when(client.checkCouponIssued("domain123"))
                .thenReturn(new IncentiveClient.CheckResult(false, false, null));
        when(client.issueCoupon("domain123"))
                .thenReturn(new IncentiveClient.IssueResult(false, null, "unverified", "HD.60630022"));

        var result = service.voucherClaim("AK", "SK", null);
        assertFalse(result.claimed());
        assertTrue(result.message().contains("实名认证"));
    }

    @Test
    void voucherClaimIssueUnknownError() {
        when(client.resolveDomainIdFromIam("AK", "SK")).thenReturn("domain123");
        when(client.checkCouponIssued("domain123"))
                .thenReturn(new IncentiveClient.CheckResult(false, false, null));
        when(client.issueCoupon("domain123"))
                .thenReturn(new IncentiveClient.IssueResult(false, null, "unknown", "HD.99999999"));

        var result = service.voucherClaim("AK", "SK", null);
        assertFalse(result.claimed());
        assertTrue(result.message().contains("发券失败"));
    }

    @Test
    void voucherClaimIamErrorReturnsGracefulMessage() {
        when(client.resolveDomainIdFromIam("AK", "SK"))
                .thenThrow(new IncentiveClient.IncentiveException("HDKIT_IAM_ERROR", "fail", null));

        var result = service.voucherClaim("AK", "SK", null);
        assertFalse(result.claimed());
        assertEquals("激励服务查询失败，请稍后重试", result.message());
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
    void voucherStatusTestMissingDomainId() {
        config.setDeployEnv("test");

        var result = service.voucherStatus("AK", "SK", null);
        assertFalse(result.claimed());
        assertEquals("测试环境需提供 domain_id", result.message());
    }

    @Test
    void voucherClaimTestMissingDomainId() {
        config.setDeployEnv("test");

        var result = service.voucherClaim("AK", "SK", null);
        assertFalse(result.claimed());
        assertEquals("测试环境需提供 domain_id", result.message());
    }

    @Test
    void voucherClaimTestSuccess() {
        config.setDeployEnv("test");

        when(client.checkCouponIssued("test-domain"))
                .thenReturn(new IncentiveClient.CheckResult(false, false, null));
        when(client.issueCoupon("test-domain"))
                .thenReturn(new IncentiveClient.IssueResult(true, "c123", null, null));

        var result = service.voucherClaim("AK", "SK", "test-domain");
        assertTrue(result.claimed());
        assertEquals("领取成功", result.message());
    }
}