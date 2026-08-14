package com.huaweicloud.hdkitservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huaweicloud.hdkitservice.config.HdkitConfig;
import com.huaweicloud.hdkitservice.util.Masker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DevStationClientTest {

    private DevStationClient client;
    private MockRestServiceServer server;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        HdkitConfig config = new HdkitConfig();
        config.setEndpoint("https://devstation.myhuaweicloud.com");
        config.setSource("CLI");
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new DevStationClient(config, mapper, new Masker(config), builder);
    }

    @Test
    void createReturnsDevStageInstanceId() {
        server.expect(requestTo("https://devstation.myhuaweicloud.com/open-api-public/v2/devenvs"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Sdk-Date", org.hamcrest.Matchers.matchesRegex("\\d{8}T\\d{6}Z")))
                .andExpect(header("Authorization", org.hamcrest.Matchers.startsWith("SDK-HMAC-SHA256 Access=TESTAK")))
                .andExpect(content().json("{\"name\":\"hcdktest1\",\"template_id\":\"tpl\",\"flavor_id\":\"flv\"}"))
                .andRespond(withSuccess(
                        "{\"result\":{\"dev_stage_instance_id\":\"abc123\"},\"error_msg\":\"success\",\"error_code\":\"0000\"}",
                        MediaType.APPLICATION_JSON));

        String id = client.create("hcdktest1", "tpl", "flv", "WEB", Map.of(), Map.of(), "TESTAK", "TESTSK");
        assertEquals("abc123", id);
        server.verify();
    }

    @Test
    void listParsesStatus() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(
                        "https://devstation.myhuaweicloud.com/open-api-public/v2/devenvs")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"result\":[{\"id\":\"abc123\",\"name\":\"hcdktest1\",\"status\":\"cde.0004\"}],"
                                + "\"error_msg\":\"success\",\"error_code\":\"0000\"}",
                        MediaType.APPLICATION_JSON));

        List<DevStationClient.Devenv> list = client.list("hcdktest1", "TESTAK", "TESTSK");
        assertEquals(1, list.size());
        assertEquals("abc123", list.get(0).id());
        assertEquals("cde.0004", list.get(0).status());
        server.verify();
    }

    @Test
    void statusOfReturnsMatchingStatus() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/open-api-public/v2/devenvs")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"result\":[{\"id\":\"a1\",\"name\":\"x\",\"status\":\"cde.0001\"},"
                                + "{\"id\":\"b2\",\"name\":\"y\",\"status\":\"cde.0002\"}],"
                                + "\"error_msg\":\"success\",\"error_code\":\"0000\"}",
                        MediaType.APPLICATION_JSON));

        assertEquals("cde.0002", client.statusOf("b2", "TESTAK", "TESTSK"));
        server.verify();
    }

    @Test
    void statusOfReturnsNullWhenNotFound() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/open-api-public/v2/devenvs")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"result\":[],\"error_msg\":\"success\",\"error_code\":\"0000\"}",
                        MediaType.APPLICATION_JSON));

        assertNull(client.statusOf("missing", "TESTAK", "TESTSK"));
        server.verify();
    }

    @Test
    void connectionsParsesConnectedAndConnecting() {
        server.expect(requestTo("https://devstation.myhuaweicloud.com/open-api-public/v1/devenvs/dev1/connections"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"result\":{\"connection_id\":373117,\"connections\":["
                                + "{\"connection_id\":373125,\"status\":\"CONNECTING\"},"
                                + "{\"connection_id\":373117,\"status\":\"CONNECTED\"}]},"
                                + "\"error_msg\":\"success\",\"error_code\":\"0000\"}",
                        MediaType.APPLICATION_JSON));

        DevStationClient.Connections conns = client.connections("dev1", "CLI", "TESTAK", "TESTSK");
        assertEquals(373117L, conns.connectionId());
        assertEquals(2, conns.list().size());
        server.verify();
    }

    @Test
    void addressReturnsUrlAndSource() {
        server.expect(requestTo("https://devstation.myhuaweicloud.com/open-api-public/v1/devenvs/dev1/connections/100"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"result\":{\"connection_id\":100,\"connection_info\":{\"extensions\":{\"source\":-2074327356},"
                                + "\"url\":\"wss://example/1\"},\"status\":\"CONNECTED\"},"
                                + "\"error_msg\":\"success\",\"error_code\":\"0000\"}",
                        MediaType.APPLICATION_JSON));

        DevStationClient.ConnectionAddress addr = client.address("dev1", 100L, "TESTAK", "TESTSK");
        assertEquals("wss://example/1", addr.url());
        assertEquals("-2074327356", addr.source());
        server.verify();
    }

    @Test
    void autoConfigReturnsStsExpiresAt() {
        server.expect(requestTo("https://devstation.myhuaweicloud.com/open-api-public/v1/auto-config"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"result\":{\"sts_expires_at\":\"2026-08-14T04:39:54Z\"},"
                                + "\"error_msg\":\"success\",\"error_code\":\"0000\"}",
                        MediaType.APPLICATION_JSON));

        assertEquals("2026-08-14T04:39:54Z", client.autoConfig("dev1", true, "TESTAK", "TESTSK"));
        server.verify();
    }

    @Test
    void upstreamErrorCodeThrowsDevStationException() {
        server.expect(requestTo("https://devstation.myhuaweicloud.com/open-api-public/v2/devenvs"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"result\":null,\"error_msg\":\"Param is invalid when create dev env.\","
                                + "\"error_code\":\"HD.98320045\"}",
                        MediaType.APPLICATION_JSON));

        DevStationClient.DevStationException ex = assertThrows(DevStationClient.DevStationException.class,
                () -> client.create("n", "t", "f", "WEB", Map.of(), Map.of(), "TESTAK", "TESTSK"));
        assertTrue(ex.getMessage().contains("HD.98320045"));
        server.verify();
    }

    @Test
    void realNameStatusParsesObjectResult() {
        server.expect(requestTo("https://devstation.myhuaweicloud.com/open-api-public/v1/realnames"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"result\":{\"realname_status\":\"2\"},\"error_msg\":\"success\",\"error_code\":\"0000\"}",
                        MediaType.APPLICATION_JSON));

        assertEquals("2", client.realNameStatus("TESTAK", "TESTSK"));
        server.verify();
    }

    @Test
    void agreementsParsesList() {
        server.expect(requestTo("https://devstation.myhuaweicloud.com/open-api-public/v1/agreements"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"result\":[{\"agr_type\":\"90102\",\"country\":\"cn\",\"language\":\"zh_cn\","
                                + "\"sign_status\":3,\"version\":2025062315},"
                                + "{\"agr_type\":\"90142\",\"country\":\"cn\",\"language\":\"zh_cn\","
                                + "\"sign_status\":1,\"version\":2026060305}],"
                                + "\"error_msg\":\"success\",\"error_code\":\"0000\"}",
                        MediaType.APPLICATION_JSON));

        List<DevStationClient.Agreement> list = client.agreements("TESTAK", "TESTSK");
        assertEquals(2, list.size());
        assertEquals("90102", list.get(0).agrType());
        assertEquals(3, list.get(0).signStatus());
        assertEquals(2025062315L, list.get(0).version());
        server.verify();
    }

    @Test
    void signAgreementsPostsSignInfoList() {
        server.expect(requestTo("https://devstation.myhuaweicloud.com/open-api-public/v1/agreements"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"sign_info_list\":["
                        + "{\"agr_type\":\"90102\",\"country\":\"cn\",\"language\":\"zh_cn\",\"version\":2025062315}]}"))
                .andRespond(withSuccess(
                        "{\"result\":true,\"error_msg\":\"success\",\"error_code\":\"0000\"}",
                        MediaType.APPLICATION_JSON));

        client.signAgreements(
                List.of(new DevStationClient.SignReq("90102", "cn", "zh_cn", 2025062315L)), "TESTAK", "TESTSK");
        server.verify();
    }
}
