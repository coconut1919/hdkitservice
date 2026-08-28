package com.huaweicloud.hdkitservice.config;

import com.huaweicloud.hdkitservice.util.Masker;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CallLogInterceptorTest {

    @Test
    void injectsTraceIdHeaderWhenMdcPresent() throws Exception {
        CallLogInterceptor interceptor = new CallLogInterceptor(new Masker(new HdkitConfig()));

        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.POST,
                URI.create("https://example.com/api"));
        byte[] body = "{\"name\":\"x\"}".getBytes(StandardCharsets.UTF_8);

        MDC.put("traceID", "trace-123");
        try {
            ClientHttpResponse resp = interceptor.intercept(request, body, (req, b) ->
                    new MockClientHttpResponse("{\"ok\":true}".getBytes(StandardCharsets.UTF_8), HttpStatus.OK));

            assertEquals("trace-123", request.getHeaders().getFirst("x-traceID"));
            assertEquals(HttpStatus.OK, resp.getStatusCode());
            assertArrayEquals("{\"ok\":true}".getBytes(StandardCharsets.UTF_8), resp.getBody().readAllBytes());
        } finally {
            MDC.remove("traceID");
        }
    }

    @Test
    void doesNotInjectTraceIdWhenMdcAbsent() throws Exception {
        CallLogInterceptor interceptor = new CallLogInterceptor(new Masker(new HdkitConfig()));

        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET,
                URI.create("https://example.com/api"));

        interceptor.intercept(request, new byte[0], (req, b) ->
                new MockClientHttpResponse(new byte[0], HttpStatus.NO_CONTENT));

        assertNull(request.getHeaders().getFirst("x-traceID"));
    }
}