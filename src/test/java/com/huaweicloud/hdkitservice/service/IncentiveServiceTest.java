package com.huaweicloud.hdkitservice.service;

import com.huaweicloud.hdkitservice.config.HdkitConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
        service = new IncentiveService(client, config);
    }

    @Test
    void voucherStatusReturnsNotClaimed() {
        when(client.resolveDomainId("AK", "SK")).thenReturn("domain123");
        when(client.checkCouponIssued("domain123"))
                .thenReturn(new IncentiveClient.CheckResult(false, false, null));

        var result = service.voucherStatus("AK", "SK");
        assertFalse(result.claimed());
        assertEquals("未领取", result.message());
    }

    @Test
    void voucherStatusReturnsClaimed() {
        when(client.resolveDomainId("AK", "SK")).thenReturn("domain123");
        when(client.checkCouponIssued("domain123"))
                .thenReturn(new IncentiveClient.CheckResult(true, false, null));

        var result = service.voucherStatus("AK", "SK");
        assertTrue(result.claimed());
        assertEquals("已领取", result.message());
    }

    @Test
    void voucherStatusHandlesServiceError() {
        when(client.resolveDomainId("AK", "SK")).thenReturn("domain123");
        when(client.checkCouponIssued("domain123"))
                .thenReturn(new IncentiveClient.CheckResult(false, true, "timeout"));

        var result = service.voucherStatus("AK", "SK");
        assertFalse(result.claimed());
        assertEquals("查询失败", result.message());
    }

    @Test
    void voucherClaimReturnsAlreadyClaimed() {
        when(client.resolveDomainId("AK", "SK")).thenReturn("domain123");
        when(client.checkCouponIssued("domain123"))
                .thenReturn(new IncentiveClient.CheckResult(true, false, null));

        var result = service.voucherClaim("AK", "SK");
        assertTrue(result.claimed());
        assertEquals("已领取过", result.message());
        verify(client, never()).issueCoupon(anyString());
    }

    @Test
    void voucherClaimCheckErrorThrows() {
        when(client.resolveDomainId("AK", "SK")).thenReturn("domain123");
        when(client.checkCouponIssued("domain123"))
                .thenReturn(new IncentiveClient.CheckResult(false, true, "timeout"));

        var ex = assertThrows(SandboxService.HdkitException.class,
                () -> service.voucherClaim("AK", "SK"));
        assertEquals("HDKIT_INCENTIVE_ERROR", ex.code());
    }

    @Test
    void voucherClaimIssueSuccess() {
        when(client.resolveDomainId("AK", "SK")).thenReturn("domain123");
        when(client.checkCouponIssued("domain123"))
                .thenReturn(new IncentiveClient.CheckResult(false, false, null));
        when(client.issueCoupon("domain123"))
                .thenReturn(new IncentiveClient.IssueResult(true, "c123", null, null));

        var result = service.voucherClaim("AK", "SK");
        assertTrue(result.claimed());
        assertEquals("c123", result.voucherId());
        assertEquals(100, result.amount());
        assertEquals("领取成功", result.message());
    }

    @Test
    void voucherClaimIssueHandlesAlreadyClaimedErrorCode() {
        when(client.resolveDomainId("AK", "SK")).thenReturn("domain123");
        when(client.checkCouponIssued("domain123"))
                .thenReturn(new IncentiveClient.CheckResult(false, false, null));
        when(client.issueCoupon("domain123"))
                .thenReturn(new IncentiveClient.IssueResult(false, null, "already", "HD.60620016"));

        var result = service.voucherClaim("AK", "SK");
        assertTrue(result.claimed());
        assertEquals("已领取过", result.message());
    }

    @Test
    void voucherClaimIssueHandlesQuotaExhausted() {
        when(client.resolveDomainId("AK", "SK")).thenReturn("domain123");
        when(client.checkCouponIssued("domain123"))
                .thenReturn(new IncentiveClient.CheckResult(false, false, null));
        when(client.issueCoupon("domain123"))
                .thenReturn(new IncentiveClient.IssueResult(false, null, "quota", "HD.60630042"));

        var ex = assertThrows(SandboxService.HdkitException.class,
                () -> service.voucherClaim("AK", "SK"));
        assertEquals("HDKIT_INCENTIVE_ERROR", ex.code());
        assertTrue(ex.getMessage().contains("已用完"));
    }

    @Test
    void voucherClaimIssueHandlesRealNameRequired() {
        when(client.resolveDomainId("AK", "SK")).thenReturn("domain123");
        when(client.checkCouponIssued("domain123"))
                .thenReturn(new IncentiveClient.CheckResult(false, false, null));
        when(client.issueCoupon("domain123"))
                .thenReturn(new IncentiveClient.IssueResult(false, null, "unverified", "HD.60630022"));

        var ex = assertThrows(SandboxService.HdkitException.class,
                () -> service.voucherClaim("AK", "SK"));
        assertEquals("HDKIT_INCENTIVE_ERROR", ex.code());
        assertTrue(ex.getMessage().contains("实名认证"));
    }
}