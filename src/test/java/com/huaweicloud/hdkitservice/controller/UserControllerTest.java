package com.huaweicloud.hdkitservice.controller;

import com.huaweicloud.hdkitservice.service.IncentiveClient;
import com.huaweicloud.hdkitservice.service.TelemetryHashService;
import com.huaweicloud.hdkitservice.util.Masker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private IncentiveClient incentiveClient;

    @MockBean
    private TelemetryHashService hashService;

    @MockBean
    private Masker masker;

    @Test
    void generatorUserIDHashWithDomainId() throws Exception {
        when(incentiveClient.resolveDomainIdFromIam("AK", "SK")).thenReturn("domain123");
        when(hashService.generateUserHash("domain123")).thenReturn("hash123abc");

        mvc.perform(get("/rest/developer/server/hdkitservice/user/generatorUserIDHash")
                        .header("X-HW-AK", "AK").header("X-HW-SK", "SK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userHash").value("hash123abc"));
    }

    @Test
    void generatorUserIDHashIamFailedFallback() throws Exception {
        when(incentiveClient.resolveDomainIdFromIam("AK", "SK"))
                .thenThrow(new IncentiveClient.IncentiveException("HDKIT_IAM_ERROR", "IAM failed", null));
        when(hashService.generateFallbackUserHash("AK")).thenReturn("fallback123");

        mvc.perform(get("/rest/developer/server/hdkitservice/user/generatorUserIDHash")
                        .header("X-HW-AK", "AK").header("X-HW-SK", "SK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userHash").value("fallback123"));
    }

    @Test
    void generatorUserIDHashDomainIdEmptyFallback() throws Exception {
        when(incentiveClient.resolveDomainIdFromIam("AK", "SK")).thenReturn("");
        when(hashService.generateFallbackUserHash("AK")).thenReturn("fallback456");

        mvc.perform(get("/rest/developer/server/hdkitservice/user/generatorUserIDHash")
                        .header("X-HW-AK", "AK").header("X-HW-SK", "SK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userHash").value("fallback456"));
    }

    @Test
    void generatorUserIDHashMissingAkHeaderReturns400() throws Exception {
        mvc.perform(get("/rest/developer/server/hdkitservice/user/generatorUserIDHash")
                        .header("X-HW-SK", "SK"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("HDKIT_INVALID_REQUEST"));
    }
}