package com.huaweicloud.hdkitservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class HdkitConfig {

    @Value("${DEVSTATION_ENDPOINT:https://devstation.myhuaweicloud.com}")
    private String endpoint;

    @Value("${DEVSTATION_SOURCE:CLI}")
    private String source;

    @Value("${TEMPLATE_ID:}")
    private String templateId;

    @Value("${FLAVOR_ID:}")
    private String flavorId;

    @Value("${POLL_INTERVAL_MS:5000}")
    private long pollIntervalMs;

    @Value("${CONNECT_TIMEOUT:300000}")
    private long connectTimeout;

    @Value("${RELEASE_TIMEOUT:180000}")
    private long releaseTimeout;

    @Value("${MAX_CONCURRENT:5}")
    private int maxConcurrent;

    @Value("${LOG_MASK_KEYS:password,passwd,pwd,secret,token,sk,access_key,private_key,authorization,cookie,credential}")
    private String maskKeys;

    public String endpoint() { return endpoint; }
    public String source() { return source; }
    public String templateId() { return templateId; }
    public String flavorId() { return flavorId; }
    public long pollIntervalMs() { return pollIntervalMs; }
    public long connectTimeout() { return connectTimeout; }
    public long releaseTimeout() { return releaseTimeout; }
    public int maxConcurrent() { return maxConcurrent; }

    public List<String> maskKeys() {
        List<String> out = new ArrayList<>();
        if (maskKeys == null || maskKeys.isBlank()) return out;
        for (String part : maskKeys.split(",")) {
            String key = part.trim().toLowerCase();
            if (!key.isEmpty()) out.add(key);
        }
        return out;
    }

    public String endpointHost() {
        return endpoint.replaceFirst("^https?://", "").replaceFirst("/$", "");
    }

    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public void setSource(String source) { this.source = source; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    public void setFlavorId(String flavorId) { this.flavorId = flavorId; }
    public void setPollIntervalMs(long pollIntervalMs) { this.pollIntervalMs = pollIntervalMs; }
    public void setConnectTimeout(long connectTimeout) { this.connectTimeout = connectTimeout; }
    public void setReleaseTimeout(long releaseTimeout) { this.releaseTimeout = releaseTimeout; }
    public void setMaxConcurrent(int maxConcurrent) { this.maxConcurrent = maxConcurrent; }
    public void setMaskKeys(String maskKeys) { this.maskKeys = maskKeys; }
}
