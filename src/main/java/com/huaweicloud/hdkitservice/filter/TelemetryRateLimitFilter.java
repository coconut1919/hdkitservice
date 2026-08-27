package com.huaweicloud.hdkitservice.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TelemetryRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TelemetryRateLimitFilter.class);

    private static final String TELEMETRY_PATH = "/rest/developer/server/hdkitservice/telemetry/events";

    private final Semaphore globalSemaphore = new Semaphore(50);

    private final Map<String, long[]> ipWindows = new ConcurrentHashMap<>();
    private final Map<String, long[]> installIdWindows = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (!TELEMETRY_PATH.equals(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        if (!globalSemaphore.tryAcquire()) {
            response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"code\":\"HDKIT_OVERLOADED\",\"message\":\"Server overloaded\"}");
            return;
        }

        try {
            String ip = getClientIp(request);
            if (!checkRate(ipWindows, ip, 10)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"code\":\"HDKIT_RATE_LIMITED\",\"message\":\"Too many requests\"}");
                return;
            }

            String installId = request.getHeader("X-Install-ID");
            if (installId != null && !installId.isEmpty()) {
                if (!checkRate(installIdWindows, installId, 2)) {
                    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                    response.setContentType("application/json");
                    response.getWriter().write("{\"code\":\"HDKIT_RATE_LIMITED\",\"message\":\"Too many requests\"}");
                    return;
                }
            }

            chain.doFilter(request, response);
        } finally {
            globalSemaphore.release();
        }
    }

    private boolean checkRate(Map<String, long[]> windows, String key, int maxPerSecond) {
        long now = System.currentTimeMillis();
        long[] entry = windows.computeIfAbsent(key, k -> new long[]{now, 0});

        synchronized (entry) {
            if (now - entry[0] >= 1000) {
                entry[0] = now;
                entry[1] = 1;
                return true;
            }
            if (entry[1] >= maxPerSecond) {
                return false;
            }
            entry[1]++;
            return true;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isEmpty()) {
            return realIp;
        }
        return request.getRemoteAddr();
    }
}