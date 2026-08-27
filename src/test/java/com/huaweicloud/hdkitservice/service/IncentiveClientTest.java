package com.huaweicloud.hdkitservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huaweicloud.hdkitservice.config.HdkitConfig;
import com.huaweicloud.hdkitservice.util.Masker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class IncentiveClientTest {

    private IncentiveClient client;
    private MockRestServiceServer server;
    private HdkitConfig config;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        config = new HdkitConfig();
        config.setDeployEnv("test");
        config.setIamDomainsEndpoint("https://iam.myhuaweicloud.com");
        config.setIncentiveCheckUrl("https://incentive.example.com/check");
        config.setIncentiveIssueUrl("https://incentive.example.com/issue");
        config.setIncentiveAppCode("APPCODE123");
        config.setIncentiveHwId("HWID");
        config.setIncentiveAppKey("APPKEY");
        config.setIncentiveAuthToken("TEST_TOKEN");
        config.setIncentiveFaceAmount(100);
        config.setIncentiveActivityId("A000330");
        config.setIncentiveActivityProductId("PRODUCT_ID");
        config.setIncentiveCurrency("CNY");

        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new IncentiveClient(config, mapper, new Masker(config), builder);
    }

    @Test
    void resolveDomainIdReturnsId() {
        server.expect(requestTo("https://iam.myhuaweicloud.com/v3/auth/domains"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization",
                        org.hamcrest.Matchers.startsWith("SDK-HMAC-SHA256 Access=TESTAK")))
                .andRespond(withSuccess(
                        "{\"domains\":[{\"id\":\"domain123\",\"name\":\"test\",\"enabled\":true}]}",
                        org.springframework.http.MediaType.APPLICATION_JSON));

        String domainId = client.resolveDomainIdFromIam("TESTAK", "TESTSK");
        assertEquals("domain123", domainId);
    }

    @Test
    void resolveDomainIdThrowsWhenNoDomains() {
        server.expect(requestTo("https://iam.myhuaweicloud.com/v3/auth/domains"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"domains\":[]}",
                        org.springframework.http.MediaType.APPLICATION_JSON));

        var ex = assertThrows(IncentiveClient.IncentiveException.class,
                () -> client.resolveDomainIdFromIam("TESTAK", "TESTSK"));
        assertEquals("HDKIT_IAM_ERROR", ex.code());
    }

    @Test
    void resolveDomainIdThrowsWhenHttpError() {
        server.expect(requestTo("https://iam.myhuaweicloud.com/v3/auth/domains"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        var ex = assertThrows(IncentiveClient.IncentiveException.class,
                () -> client.resolveDomainIdFromIam("TESTAK", "TESTSK"));
        assertEquals("HDKIT_IAM_ERROR", ex.code());
    }

    @Test
    void checkCouponIssuedReturnsNotIssued() {
        server.expect(requestTo("https://incentive.example.com/check"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-APIG-APPCODE", "APPCODE123"))
                .andExpect(header("X-HW-ID", "HWID"))
                .andExpect(header("X-auth-token", "TEST_TOKEN"))
                .andRespond(withSuccess(
                        "{\"issued_tag\":0,\"error_code\":\"0000\"}",
                        org.springframework.http.MediaType.APPLICATION_JSON));

        var result = client.checkCouponIssued("domain123");
        assertFalse(result.issued());
        assertFalse(result.serviceError());
    }

    @Test
    void checkCouponIssuedReturnsIssued() {
        server.expect(requestTo("https://incentive.example.com/check"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"issued_tag\":1,\"error_code\":\"0000\"}",
                        org.springframework.http.MediaType.APPLICATION_JSON));

        var result = client.checkCouponIssued("domain123");
        assertTrue(result.issued());
        assertFalse(result.serviceError());
    }

    @Test
    void checkCouponIssuedHandlesServiceError() {
        server.expect(requestTo("https://incentive.example.com/check"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"error_code\":\"HD.60620016\",\"error_msg\":\"already claimed\"}",
                        org.springframework.http.MediaType.APPLICATION_JSON));

        var result = client.checkCouponIssued("domain123");
        assertTrue(result.serviceError());
        assertEquals("already claimed", result.error());
    }

    @Test
    void issueCouponReturnsSuccess() {
        server.expect(requestTo("https://incentive.example.com/issue"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-APIG-APPCODE", "APPCODE123"))
                .andRespond(withSuccess(
                        "{\"coupon_id\":\"c123\",\"error_code\":\"0000\"}",
                        org.springframework.http.MediaType.APPLICATION_JSON));

        var result = client.issueCoupon("domain123");
        assertTrue(result.success());
        assertEquals("c123", result.couponId());
    }

    @Test
    void issueCouponHandlesErrorCode() {
        server.expect(requestTo("https://incentive.example.com/issue"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"error_code\":\"HD.60630042\",\"error_msg\":\"quota exhausted\"}",
                        org.springframework.http.MediaType.APPLICATION_JSON));

        var result = client.issueCoupon("domain123");
        assertFalse(result.success());
        assertEquals("HD.60630042", result.errorCode());
        assertEquals("quota exhausted", result.error());
    }

    @Test
    void issueCouponReturnsFailWhenNoCouponId() {
        server.expect(requestTo("https://incentive.example.com/issue"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"error_code\":\"0000\"}",
                        org.springframework.http.MediaType.APPLICATION_JSON));

        var result = client.issueCoupon("domain123");
        assertFalse(result.success());
        assertEquals("发券失败", result.error());
    }

    @Test
    void issueCouponHandlesHttp400BusinessError() {
        server.expect(requestTo("https://incentive.example.com/issue"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .body("{\"error_code\":\"HD.60720044\",\"error_msg\":\"data is null error, cbc getCustomerInfo error.\"}"));

        var result = client.issueCoupon("domain123");
        assertFalse(result.success());
        assertEquals("HD.60720044", result.errorCode());
        assertEquals("data is null error, cbc getCustomerInfo error.", result.error());
    }

    @Test
    void issueCouponReturnsCouponIdFromDataField() {
        server.expect(requestTo("https://incentive.example.com/issue"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"data\":{\"coupon_id\":\"c456\"},\"error_code\":\"0000\"}",
                        org.springframework.http.MediaType.APPLICATION_JSON));

        var result = client.issueCoupon("domain123");
        assertTrue(result.success());
        assertEquals("c456", result.couponId());
    }

    @Test
    void issueCouponThrowsWhenFaceAmountNotConfigured() {
        config.setIncentiveFaceAmount(0);
        IncentiveClient clientZero = new IncentiveClient(config, mapper, new Masker(config),
                RestClient.builder());

        var ex = assertThrows(IncentiveClient.IncentiveException.class,
                () -> clientZero.issueCoupon("domain123"));
        assertEquals("HDKIT_INCENTIVE_ERROR", ex.code());
        assertEquals("代金券面额未配置", ex.getMessage());
    }
}