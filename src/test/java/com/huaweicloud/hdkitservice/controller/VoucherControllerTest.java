package com.huaweicloud.hdkitservice.controller;

import com.huaweicloud.hdkitservice.service.IncentiveService;
import com.huaweicloud.hdkitservice.util.Masker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VoucherController.class)
class VoucherControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private IncentiveService incentiveService;

    @MockBean
    private Masker masker;

    @Test
    void voucherStatusNotClaimed() throws Exception {
        when(incentiveService.voucherStatus(eq("AK"), eq("SK"), any()))
                .thenReturn(new IncentiveService.VoucherStatusResult(false, "未领取"));

        mvc.perform(get("/rest/developer/server/hdkitservice/voucher/status")
                        .header("X-HW-AK", "AK").header("X-HW-SK", "SK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claimed").value(false))
                .andExpect(jsonPath("$.message").value("未领取"));
    }

    @Test
    void voucherStatusWithDomainId() throws Exception {
        when(incentiveService.voucherStatus(eq("AK"), eq("SK"), eq("test-domain")))
                .thenReturn(new IncentiveService.VoucherStatusResult(false, "未领取"));

        mvc.perform(get("/rest/developer/server/hdkitservice/voucher/status")
                        .header("X-HW-AK", "AK").header("X-HW-SK", "SK")
                        .param("domain_id", "test-domain"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claimed").value(false));
    }

    @Test
    void voucherClaimSuccess() throws Exception {
        when(incentiveService.voucherClaim(eq("AK"), eq("SK"), any()))
                .thenReturn(new IncentiveService.VoucherClaimResult(true, "v123", 100, "领取成功"));

        mvc.perform(post("/rest/developer/server/hdkitservice/voucher/claim")
                        .header("X-HW-AK", "AK").header("X-HW-SK", "SK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claimed").value(true))
                .andExpect(jsonPath("$.voucherId").value("v123"))
                .andExpect(jsonPath("$.amount").value(100))
                .andExpect(jsonPath("$.message").value("领取成功"));
    }

    @Test
    void voucherClaimQuotaExhaustedReturns200() throws Exception {
        when(incentiveService.voucherClaim(eq("AK"), eq("SK"), any()))
                .thenReturn(new IncentiveService.VoucherClaimResult(false, null, 0,
                        "本月代金券总额度已用完，请下月再试"));

        mvc.perform(post("/rest/developer/server/hdkitservice/voucher/claim")
                        .header("X-HW-AK", "AK").header("X-HW-SK", "SK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claimed").value(false))
                .andExpect(jsonPath("$.message").value("本月代金券总额度已用完，请下月再试"));
    }

    @Test
    void voucherClaimWithDomainId() throws Exception {
        when(incentiveService.voucherClaim(eq("AK"), eq("SK"), eq("test-domain")))
                .thenReturn(new IncentiveService.VoucherClaimResult(false, null, 0, "未领取"));

        mvc.perform(post("/rest/developer/server/hdkitservice/voucher/claim")
                        .header("X-HW-AK", "AK").header("X-HW-SK", "SK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"domain_id\":\"test-domain\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claimed").value(false));
    }

    @Test
    void voucherClaimMissingAkHeaderReturns400() throws Exception {
        mvc.perform(post("/rest/developer/server/hdkitservice/voucher/claim")
                        .header("X-HW-SK", "SK"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("HDKIT_INVALID_REQUEST"));
    }
}