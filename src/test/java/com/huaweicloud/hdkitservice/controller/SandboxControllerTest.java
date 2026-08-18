package com.huaweicloud.hdkitservice.controller;

import com.huaweicloud.hdkitservice.model.CheckUserResponse;
import com.huaweicloud.hdkitservice.model.ConnectResponse;
import com.huaweicloud.hdkitservice.model.CredentialsResponse;
import com.huaweicloud.hdkitservice.model.SignAgreementResponse;
import com.huaweicloud.hdkitservice.service.SandboxService;
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

@WebMvcTest(SandboxController.class)
class SandboxControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private SandboxService service;

    @MockBean
    private Masker masker;

    @Test
    void connectEndpoint() throws Exception {
        when(service.connect(any(), eq("AK"), eq("SK")))
                .thenReturn(new ConnectResponse("s1", "dev1", "100", "wss://x", "connected"));

        mvc.perform(post("/rest/developer/server/hdkitservice/connect")
                        .header("X-HW-AK", "AK").header("X-HW-SK", "SK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"n1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("s1"))
                .andExpect(jsonPath("$.connectionAddress").value("wss://x"));
    }

    @Test
    void connectMissingAkHeaderReturns400() throws Exception {
        mvc.perform(post("/rest/developer/server/hdkitservice/connect")
                        .header("X-HW-SK", "SK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"n1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("HDKIT_INVALID_REQUEST"));
    }

    @Test
    void credentialsEndpoint() throws Exception {
        when(service.credentials(any(), eq("AK"), eq("SK")))
                .thenReturn(new CredentialsResponse("s1", "2026-08-14T04:39:54Z"));

        mvc.perform(post("/rest/developer/server/hdkitservice/credentials")
                        .header("X-HW-AK", "AK").header("X-HW-SK", "SK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dev_stage_id\":\"dev1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiresAt").value("2026-08-14T04:39:54Z"));
    }

    @Test
    void hdkitExceptionMappedToHttpStatus() throws Exception {
        when(service.credentials(any(), eq("AK"), eq("SK")))
                .thenThrow(new SandboxService.HdkitException("HDKIT_NOT_RUNNING", "环境未处于 RUNNING", null));

        mvc.perform(post("/rest/developer/server/hdkitservice/credentials")
                        .header("X-HW-AK", "AK").header("X-HW-SK", "SK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dev_stage_id\":\"dev1\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("HDKIT_NOT_RUNNING"));
    }

    @Test
    void checkUserEndpoint() throws Exception {
        when(service.checkUser(eq("AK"), eq("SK")))
                .thenReturn(new CheckUserResponse(true, true));

        mvc.perform(get("/rest/developer/server/hdkitservice/check-user")
                        .header("X-HW-AK", "AK").header("X-HW-SK", "SK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.realnameVerified").value(true))
                .andExpect(jsonPath("$.agreementSigned").value(true));
    }

    @Test
    void checkUserNotRealnameMappedTo403() throws Exception {
        when(service.checkUser(eq("AK"), eq("SK")))
                .thenThrow(new SandboxService.HdkitException("HDKIT_NOT_REALNAME", "用户未完成实名认证", null));

        mvc.perform(get("/rest/developer/server/hdkitservice/check-user")
                        .header("X-HW-AK", "AK").header("X-HW-SK", "SK"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("HDKIT_NOT_REALNAME"));
    }

    @Test
    void checkUserBothMissingMappedTo403CombinedCode() throws Exception {
        when(service.checkUser(eq("AK"), eq("SK")))
                .thenThrow(new SandboxService.HdkitException(
                        "HDKIT_NOT_REALNAME_AND_AGREEMENT", "用户未完成实名认证且未签署最新版协议", null));

        mvc.perform(get("/rest/developer/server/hdkitservice/check-user")
                        .header("X-HW-AK", "AK").header("X-HW-SK", "SK"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("HDKIT_NOT_REALNAME_AND_AGREEMENT"));
    }

    @Test
    void signAgreementEndpoint() throws Exception {
        when(service.signAgreement(eq("AK"), eq("SK")))
                .thenReturn(new SignAgreementResponse(true, 3));

        mvc.perform(post("/rest/developer/server/hdkitservice/sign-agreement")
                        .header("X-HW-AK", "AK").header("X-HW-SK", "SK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.signed").value(true))
                .andExpect(jsonPath("$.signedCount").value(3));
    }
}
