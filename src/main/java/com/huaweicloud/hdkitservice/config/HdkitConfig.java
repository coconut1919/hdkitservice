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

    // ── 激励金服务 ──
    @Value("${INCENTIVE_CHECK_URL:}")
    private String incentiveCheckUrl;

    @Value("${INCENTIVE_ISSUE_URL:}")
    private String incentiveIssueUrl;

    @Value("${INCENTIVE_APPCODE:}")
    private String incentiveAppCode;

    @Value("${INCENTIVE_HW_ID:}")
    private String incentiveHwId;

    @Value("${INCENTIVE_APPKEY:}")
    private String incentiveAppKey;

    @Value("${INCENTIVE_AUTH_TOKEN:}")
    private String incentiveAuthToken;

    @Value("${INCENTIVE_FACE_AMOUNT:500}")
    private int incentiveFaceAmount;

    @Value("${INCENTIVE_ACTIVITY_ID:A000330}")
    private String incentiveActivityId;

    @Value("${INCENTIVE_ACTIVITY_PRODUCT_ID:5649bf1d2bc74d648ac6cd5496ebba91}")
    private String incentiveActivityProductId;

    @Value("${INCENTIVE_CURRENCY:CNY}")
    private String incentiveCurrency;

    @Value("${INCENTIVE_IAM_ENDPOINT:https://iam.cn-north-4.myhuaweicloud.com/v3/auth/tokens}")
    private String incentiveIamEndpoint;

    @Value("${INCENTIVE_IAM_USERNAME:}")
    private String incentiveIamUsername;

    @Value("${INCENTIVE_IAM_PASSWORD:}")
    private String incentiveIamPassword;

    @Value("${INCENTIVE_IAM_DOMAIN_NAME:}")
    private String incentiveIamDomainName;

    @Value("${IAM_DOMAINS_ENDPOINT:https://iam.myhuaweicloud.com}")
    private String iamDomainsEndpoint;

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

    public String incentiveCheckUrl() { return incentiveCheckUrl; }
    public String incentiveIssueUrl() { return incentiveIssueUrl; }
    public String incentiveAppCode() { return incentiveAppCode; }
    public String incentiveHwId() { return incentiveHwId; }
    public String incentiveAppKey() { return incentiveAppKey; }
    public String incentiveAuthToken() { return incentiveAuthToken; }
    public int incentiveFaceAmount() { return incentiveFaceAmount; }
    public String incentiveActivityId() { return incentiveActivityId; }
    public String incentiveActivityProductId() { return incentiveActivityProductId; }
    public String incentiveCurrency() { return incentiveCurrency; }
    public String incentiveIamEndpoint() { return incentiveIamEndpoint; }
    public String incentiveIamUsername() { return incentiveIamUsername; }
    public String incentiveIamPassword() { return incentiveIamPassword; }
    public String incentiveIamDomainName() { return incentiveIamDomainName; }
    public String iamDomainsEndpoint() { return iamDomainsEndpoint; }

    public void setIncentiveCheckUrl(String v) { this.incentiveCheckUrl = v; }
    public void setIncentiveIssueUrl(String v) { this.incentiveIssueUrl = v; }
    public void setIncentiveAppCode(String v) { this.incentiveAppCode = v; }
    public void setIncentiveHwId(String v) { this.incentiveHwId = v; }
    public void setIncentiveAppKey(String v) { this.incentiveAppKey = v; }
    public void setIncentiveAuthToken(String v) { this.incentiveAuthToken = v; }
    public void setIncentiveFaceAmount(int v) { this.incentiveFaceAmount = v; }
    public void setIncentiveActivityId(String v) { this.incentiveActivityId = v; }
    public void setIncentiveActivityProductId(String v) { this.incentiveActivityProductId = v; }
    public void setIncentiveCurrency(String v) { this.incentiveCurrency = v; }
    public void setIncentiveIamEndpoint(String v) { this.incentiveIamEndpoint = v; }
    public void setIncentiveIamUsername(String v) { this.incentiveIamUsername = v; }
    public void setIncentiveIamPassword(String v) { this.incentiveIamPassword = v; }
    public void setIncentiveIamDomainName(String v) { this.incentiveIamDomainName = v; }
    public void setIamDomainsEndpoint(String v) { this.iamDomainsEndpoint = v; }
}
