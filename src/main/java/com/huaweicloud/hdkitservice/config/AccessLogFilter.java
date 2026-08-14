package com.huaweicloud.hdkitservice.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class AccessLogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("com.huaweicloud.hdkitservice.interface");
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        MDC.put("requestId", requestId);
        ContentCachingRequestWrapper wrapped = new ContentCachingRequestWrapper(request);
        long start = System.currentTimeMillis();
        try {
            chain.doFilter(wrapped, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            String body = new String(wrapped.getContentAsByteArray(), StandardCharsets.UTF_8);
            log.info("[interface] {} {} {} status={} dur={}ms ak={} {}",
                    requestId,
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    duration,
                    mask(request.getHeader("X-HW-AK")),
                    summarize(body));
            MDC.remove("requestId");
        }
    }

    private String mask(String ak) {
        if (ak == null || ak.isEmpty()) return "-";
        if (ak.length() <= 8) return "***";
        return ak.substring(0, 4) + "***" + ak.substring(ak.length() - 4);
    }

    private String summarize(String body) {
        if (body == null || body.isBlank()) return "";
        try {
            JsonNode root = mapper.readTree(body);
            StringBuilder sb = new StringBuilder();
            append(sb, root, "template_id");
            append(sb, root, "flavor_id");
            append(sb, root, "session_id");
            append(sb, root, "dev_stage_id");
            append(sb, root, "source");
            return sb.toString().trim();
        } catch (Exception e) {
            return "";
        }
    }

    private void append(StringBuilder sb, JsonNode root, String field) {
        JsonNode v = root.get(field);
        if (v != null && v.isTextual() && !v.asText().isEmpty()) {
            sb.append(field).append("=").append(v.asText()).append(" ");
        }
    }
}
