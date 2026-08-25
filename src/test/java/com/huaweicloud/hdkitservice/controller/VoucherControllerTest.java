package com.huaweicloud.hdkitservice.controller;

import com.huaweicloud.hdkitservice.service.IncentiveService;
import com.huaweicloud.hdkitservice.service.SandboxService;
import com.huaweicloud.hdkitservice.util.Masker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

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
    void voucherStatusEndpoint() throws Exception {
        when(incentiveService.voucherStatus(eq("AK"), eq("SK")))
                .thenReturn(new IncentiveService.VoucherStatusResult(false, "未领取"));

        mvc.perform(get("/rest/developer/server/hdkitservice/voucher/status")
                        .header("X-HW-AK", "AK").header("X-HW-SK", "SK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claimed").value(false))
                .andExpect(jsonPath("$.message").value("未领取"));
    }

    @Test
    void voucherClaimEndpoint() throws Exception {
        when(incentiveService.voucherClaim(eq("AK"), eq("SK")))
                .thenReturn(new IncentiveService.VoucherClaimResult(true, "v123", 100, "领取成功"));

        mvc.perform(post("/rest/developer/server/hdkitservice/voucher/claim")
                        .header("X-HW-AK", "AK").header("X-HW-SK", "SK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claimed").value(true))
                .andExpect(jsonPath("$.voucherId").value("v123"))
                .andExpect(jsonPath("$.amount").value(100));
    }

    @Test
    void voucherClaimIncentiveErrorMappedTo502() throws Exception {
        when(incentiveService.voucherClaim(eq("AK"), eq("SK")))
                .thenThrow(new SandboxService.HdkitException("HDKIT_INCENTIVE_ERROR",
                        "激励服务查询失败，请稍后重试", null));

        mvc.perform(post("/rest/developer/server/hdkitservice/voucher/claim")
                        .header("X-HW-AK", "AK").header("X-HW-SK", "SK"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("HDKIT_INCENTIVE_ERROR"));
    }

    @Test
    void voucherClaimMissingAkHeaderReturns400() throws Exception {
        mvc.perform(post("/rest/developer/server/hdkitservice/voucher/claim")
                        .header("X-HW-SK", "SK"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("HDKIT_INVALID_REQUEST"));
    }
}