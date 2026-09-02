package com.huaweicloud.hdkitservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huaweicloud.hdkitservice.config.HdkitConfig;
import com.huaweicloud.hdkitservice.sign.Signer;
import com.huaweicloud.hdkitservice.util.Masker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class IncentiveClient {

    private static final Logger log = LoggerFactory.getLogger(IncentiveClient.class);

    private final HdkitConfig config;
    private final ObjectMapper mapper;
    private final RestClient restClient;
    private final Masker masker;

    private String cachedIamToken;
    private long cachedIamTokenExpiry;

    @Autowired
    public IncentiveClient(HdkitConfig config, ObjectMapper mapper, Masker masker, RestClient restClient) {
        this.config = config;
        this.mapper = mapper;
        this.masker = masker;
        this.restClient = restClient;
    }

    // ── IAM: 获取 domainId ──

    public String resolveDomainIdFromIam(String ak, String sk, String securityToken) {
        String host = iamDomainsHost();
        Signer.SignResult sr = Signer.signWithToken("GET", "/v3/auth/domains", "", "", ak, sk, securityToken, host);
        String url = config.iamDomainsEndpoint() + "/v3/auth/domains";

        log.info("[incentive] resolving domainId via IAM");
        try {
            var req = restClient.method(HttpMethod.GET)
                    .uri(url)
                    .header("Authorization", sr.authorization())
                    .header("X-Sdk-Date", sr.timestamp())
                    .header("Host", host);
            if (securityToken != null && !securityToken.isEmpty()) {
                req.header("X-Security-Token", securityToken);
            }
            String resp = req.retrieve().body(String.class);
            JsonNode root = mapper.readTree(resp);
            JsonNode domains = root.path("domains");
            if (domains.isArray() && domains.size() > 0) {
                String domainId = domains.get(0).path("id").asText();
                log.info("[incentive] domainId resolved: {}", masker.mask(domainId));
                return domainId;
            }
            throw new IncentiveException("HDKIT_IAM_ERROR",
                    "IAM 未返回 domain 信息，请确认 AK/SK 有效", null);
        } catch (IncentiveException e) {
            throw e;
        } catch (Exception e) {
            log.error("[incentive] resolveDomainId failed: {}", e.getMessage());
            throw new IncentiveException("HDKIT_IAM_ERROR",
                    "获取华为云账号 ID 失败: " + e.getMessage(), e);
        }
    }

    // ── 激励 API: 查券 ──

    public CheckResult checkCouponIssued(String domainId) {
        String body = buildJson(ob -> ob.put("customer_id", domainId).put("scene_type", 40));

        try {
            JsonNode data = incentiveRequest(config.incentiveCheckUrl(), body);
            String errorCode = data.path("error_code").asText();
            if (!errorCode.isEmpty() && !"0000".equals(errorCode)) {
                String errorMsg = data.path("error_msg").asText();
                log.error("[incentive] check error: {} {}", errorCode, errorMsg);
                return new CheckResult(false, true, errorMsg);
            }
            boolean issued = data.path("issued_tag").asInt() == 1;
            log.info("[incentive] check-coupon RESPONSE → issued_tag={}", issued ? 1 : 0);
            return new CheckResult(issued, false, null);
        } catch (IncentiveException e) {
            throw e;
        } catch (Exception e) {
            log.error("[incentive] check failed: {}", e.getMessage());
            return new CheckResult(false, true, e.getMessage());
        }
    }

    // ── 激励 API: 发券 ──

    public IssueResult issueCoupon(String domainId) {
        int rawAmount = config.incentiveFaceAmount();
        if (rawAmount <= 0) {
            throw new IncentiveException("HDKIT_INCENTIVE_ERROR", "代金券面额未配置", null);
        }
        int faceAmount = Math.min(rawAmount, 500);
        String body = buildJson(ob -> {
            ob.put("customer_id", domainId);
            ob.put("activity_id", config.incentiveActivityId());
            ob.put("activity_product_id", config.incentiveActivityProductId());
            ob.put("face_amount", String.valueOf(faceAmount));
            ob.put("currency_code", config.incentiveCurrency());
            ob.put("is_send_notify", "0");
            ob.put("service_resource_type", 1);
        });

        try {
            JsonNode data = incentiveRequest(config.incentiveIssueUrl(), body);
            String errorCode = data.path("error_code").asText();
            if (!errorCode.isEmpty() && !"0000".equals(errorCode)) {
                String errorMsg = data.path("error_msg").asText();
                log.error("[incentive] issue error: {} {}", errorCode, errorMsg);
                return new IssueResult(false, null, errorMsg, errorCode);
            }
            String couponId = data.has("coupon_id") ? data.path("coupon_id").asText()
                    : data.path("data").path("coupon_id").asText();
            if (couponId == null || couponId.isEmpty()) {
                log.error("[incentive] issue response missing coupon_id");
                return new IssueResult(false, null, "发券失败", null);
            }
            log.info("[incentive] issue-coupon RESPONSE → coupon_id={}", masker.mask(couponId));
            return new IssueResult(true, couponId, null, null);
        } catch (IncentiveException e) {
            throw e;
        } catch (Exception e) {
            log.error("[incentive] issue failed: {}", e.getMessage());
            return new IssueResult(false, null, e.getMessage(), null);
        }
    }

    // ── 私有方法 ──

    private JsonNode incentiveRequest(String url, String body) {
        try {
            String token = getAuthToken();
            String appCode = config.incentiveAppCode();
            ResponseEntity<String> resp = restClient.method(HttpMethod.POST)
                    .uri(url)
                    .header("X-APIG-APPCODE", appCode)
                    .header("X-HW-ID", config.incentiveHwId())
                    .header("X-HW-APPKEY", config.incentiveAppKey())
                    .header("X-auth-token", token)
                    .body(body)
                    .exchange((request, response) -> new ResponseEntity<>(
                            response.bodyTo(String.class), response.getHeaders(), response.getStatusCode()));
            HttpStatusCode status = resp.getStatusCode();
            String respBody = resp.getBody();
            if (status.isError()) {
                JsonNode node = null;
                try {
                    node = mapper.readTree(respBody);
                } catch (Exception ignore) {
                    // 响应体非 JSON，按真正的服务故障处理
                }
                if (node != null && node.has("error_code")
                        && !node.path("error_code").asText().isEmpty()) {
                    return node;
                }
                throw new IllegalStateException("激励服务 HTTP " + status.value()
                        + (respBody != null && !respBody.isEmpty() ? ": " + respBody : ""));
            }
            return mapper.readTree(respBody);
        } catch (Exception e) {
            throw new IncentiveException("HDKIT_INCENTIVE_ERROR",
                    "激励服务调用失败: " + e.getMessage(), e);
        }
    }

    private String getAuthToken() {
        if ("test".equalsIgnoreCase(config.deployEnv())) {
            return config.incentiveAuthToken();
        }
        if (cachedIamToken != null && System.currentTimeMillis() < cachedIamTokenExpiry - 600000) {
            return cachedIamToken;
        }
        return fetchIamToken();
    }

    private String fetchIamToken() {
        String endpoint = config.incentiveIamEndpoint();
        log.info("[incentive] fetching IAM token from {}", endpoint);
        try {
            String body = buildJson(ob -> {
                var auth = ob.putObject("auth");
                var identity = auth.putObject("identity");
                identity.putArray("methods").add("password");
                var password = identity.putObject("password");
                var user = password.putObject("user");
                var domain = user.putObject("domain");
                domain.put("name", config.incentiveIamDomainName());
                user.put("name", config.incentiveIamUsername());
                user.put("password", config.incentiveIamPassword());
            });

            var respEntity = restClient.method(HttpMethod.POST)
                    .uri(endpoint)
                    .body(body)
                    .retrieve()
                    .toEntity(String.class);

            String token = respEntity.getHeaders().getFirst("X-Subject-Token");
            if (token == null || token.isEmpty()) {
                throw new IllegalStateException("IAM response missing X-Subject-Token header");
            }

            String respBody = respEntity.getBody();
            if (respBody != null && !respBody.isEmpty()) {
                JsonNode root = mapper.readTree(respBody);
                if (root.has("token") && root.get("token").has("expires_at")) {
                    String expiresAt = root.get("token").get("expires_at").asText();
                    cachedIamTokenExpiry = java.time.Instant.parse(expiresAt).toEpochMilli();
                } else {
                    cachedIamTokenExpiry = System.currentTimeMillis() + 23 * 3600 * 1000L;
                }
            } else {
                cachedIamTokenExpiry = System.currentTimeMillis() + 23 * 3600 * 1000L;
            }
            cachedIamToken = token;
            log.info("[incentive] IAM token acquired, expires at {}",
                    java.time.Instant.ofEpochMilli(cachedIamTokenExpiry));
            return token;
        } catch (IncentiveException e) {
            throw e;
        } catch (Exception e) {
            throw new IncentiveException("HDKIT_IAM_ERROR",
                    "获取 IAM Token 失败: " + e.getMessage(), e);
        }
    }

    private String iamDomainsHost() {
        String ep = config.iamDomainsEndpoint();
        return ep.replaceFirst("^https?://", "").replaceFirst("/$", "");
    }

    private String buildJson(java.util.function.Consumer<com.fasterxml.jackson.databind.node.ObjectNode> fn) {
        var node = mapper.createObjectNode();
        fn.accept(node);
        return node.toString();
    }

    // ── 结果类 ──

    public record CheckResult(boolean issued, boolean serviceError, String error) {}
    public record IssueResult(boolean success, String couponId, String error, String errorCode) {}

    // ── 异常类 ──

    public static class IncentiveException extends RuntimeException {
        private final String code;
        public IncentiveException(String code, String message, Throwable cause) {
            super(message, cause);
            this.code = code;
        }
        public String code() { return code; }
    }
}