package com.huaweicloud.hdkitservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huaweicloud.hdkitservice.config.HdkitConfig;
import com.huaweicloud.hdkitservice.sign.Signer;
import com.huaweicloud.hdkitservice.util.Masker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class DevStationClient {

    private static final Logger log = LoggerFactory.getLogger(DevStationClient.class);
    private static final Logger callLog = LoggerFactory.getLogger("com.huaweicloud.hdkitservice.call");

    private final HdkitConfig config;
    private final ObjectMapper mapper;
    private final Masker masker;
    private final RestClient restClient;

    @Autowired
    public DevStationClient(HdkitConfig config, ObjectMapper mapper, Masker masker) {
        this(config, mapper, masker, RestClient.builder());
    }

    DevStationClient(HdkitConfig config, ObjectMapper mapper, Masker masker, RestClient.Builder builder) {
        this.config = config;
        this.mapper = mapper;
        this.masker = masker;
        this.restClient = builder.baseUrl(config.endpoint())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    private JsonNode call(String method, String path, String query, String body, String ak, String sk) {
        Signer.SignResult sr = Signer.sign(method, path, query, body == null ? "" : body,
                ak, sk, config.endpointHost());
        String uri = path + (query.isEmpty() ? "" : "?" + query);
        String requestId = MDC.get("requestId");
        long start = System.currentTimeMillis();
        callLog.info("[call] {} {} {} req={}", requestId, method, uri, masker.mask(body == null ? "" : body));
        try {
            var spec = restClient.method(HttpMethod.valueOf(method))
                    .uri(uri)
                    .header("Authorization", sr.authorization())
                    .header("X-Sdk-Date", sr.timestamp());
            if (body != null && !body.isEmpty()) {
                spec = spec.body(body);
            }
            String resp = spec.retrieve().body(String.class);
            JsonNode root = mapper.readTree(resp);
            String errorCode = root.path("error_code").asText();
            long duration = System.currentTimeMillis() - start;
            if (!errorCode.isEmpty() && !"0000".equals(errorCode)) {
                String errorMsg = root.path("error_msg").asText();
                callLog.error("[call] {} {} {} <- upstream_error code={} msg={} resp={} dur={}ms",
                        requestId, method, path, errorCode, errorMsg, masker.mask(resp), duration);
                log.error("[devstation] {} {} upstream error {}: {}", method, path, errorCode, errorMsg);
                throw new DevStationException(method + " " + path + " upstream error " + errorCode
                        + ": " + errorMsg, null);
            }
            callLog.info("[call] {} {} {} <- ok dur={}ms resp={}",
                    requestId, method, path, duration, masker.mask(resp));
            return root;
        } catch (DevStationException e) {
            throw e;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            callLog.error("[call] {} {} {} <- failed err={} dur={}ms",
                    requestId, method, path, masker.mask(e.getMessage()), duration);
            log.error("[devstation] {} {} failed: {}", method, path, e.getMessage());
            throw new DevStationException(method + " " + path + " failed", e);
        }
    }

    public String create(String name, String templateId, String flavorId, String source,
                         Map<String, String> env, Map<String, String> git, String ak, String sk) {
        var body = mapper.createObjectNode();
        body.put("name", name);
        body.put("template_id", templateId);
        body.put("flavor_id", flavorId);
        body.put("source", source == null ? "WEB" : source);
        var envNode = body.putObject("env");
        if (env != null) env.forEach(envNode::put);
        var gitNode = body.putObject("git");
        if (git != null) git.forEach(gitNode::put);
        else {
            gitNode.put("repo_url", "").put("repo_branch", "").put("repo_name", "")
                    .put("target_path", "").put("open_type", "");
        }
        JsonNode r = call("POST", "/open-api-public/v2/devenvs", "", body.toString(), ak, sk);
        return r.path("result").path("dev_stage_instance_id").asText();
    }

    public List<Devenv> list(String nameEq, String ak, String sk) {
        String query = (nameEq == null || nameEq.isEmpty()) ? "" : "name_eq=" + nameEq;
        JsonNode r = call("GET", "/open-api-public/v2/devenvs", query, "", ak, sk);
        List<Devenv> out = new ArrayList<>();
        for (JsonNode item : r.path("result")) {
            out.add(new Devenv(item.path("id").asText(), item.path("name").asText(),
                    item.path("status").asText()));
        }
        return out;
    }

    public String statusOf(String devStageId, String ak, String sk) {
        for (Devenv d : list("", ak, sk)) {
            if (d.id().equals(devStageId)) return d.status();
        }
        return null;
    }

    public void start(String devStageId, String source, String ak, String sk) {
        String body = "{\"source\":\"" + source + "\"}";
        call("POST", "/open-api-public/v1/devenvs/" + devStageId + "/start", "", body, ak, sk);
    }

    public void close(String devStageId, String source, String ak, String sk) {
        String body = "{\"source\":\"" + source + "\"}";
        call("POST", "/open-api-public/v1/devenvs/" + devStageId + "/close", "", body, ak, sk);
    }

    public void delete(String devStageId, String source, String ak, String sk) {
        call("DELETE", "/open-api-public/v1/devenvs/" + devStageId, "source=" + source, "", ak, sk);
    }

    public Connections connections(String devStageId, String source, String ak, String sk) {
        String body = "{\"source\":\"" + source + "\"}";
        JsonNode r = call("POST", "/open-api-public/v1/devenvs/" + devStageId + "/connections", "", body, ak, sk);
        JsonNode result = r.path("result");
        long main = result.path("connection_id").asLong();
        List<Conn> list = new ArrayList<>();
        for (JsonNode c : result.path("connections")) {
            list.add(new Conn(c.path("connection_id").asLong(), c.path("status").asText()));
        }
        return new Connections(main, list);
    }

    public ConnectionAddress address(String devStageId, long connectionId, String ak, String sk) {
        JsonNode r = call("GET", "/open-api-public/v1/devenvs/" + devStageId
                + "/connections/" + connectionId, "", "", ak, sk);
        JsonNode info = r.path("result").path("connection_info");
        String url = info.path("url").asText();
        String source = info.path("extensions").path("source").asText();
        return new ConnectionAddress(url, source);
    }

    public String autoConfig(String devStageId, boolean enableSts, String ak, String sk) {
        String body = "{\"instance_id\":\"" + devStageId + "\",\"enable_sts\":" + enableSts + "}";
        JsonNode r = call("POST", "/open-api-public/v1/auto-config", "", body, ak, sk);
        return r.path("result").path("sts_expires_at").asText();
    }

    public String realNameStatus(String ak, String sk) {
        JsonNode r = call("GET", "/open-api-public/v1/realnames", "", "", ak, sk);
        JsonNode result = r.path("result");
        if (result.isArray() && result.size() > 0) {
            return result.get(0).path("realname_status").asText();
        }
        return result.path("realname_status").asText();
    }

    public List<Agreement> agreements(String ak, String sk) {
        JsonNode r = call("GET", "/open-api-public/v1/agreements", "", "", ak, sk);
        List<Agreement> out = new ArrayList<>();
        for (JsonNode item : r.path("result")) {
            out.add(new Agreement(
                    item.path("agr_type").asText(),
                    item.path("country").asText("cn"),
                    item.path("language").asText("zh_cn"),
                    item.path("sign_status").asInt(-1),
                    item.path("version").asLong(0)));
        }
        return out;
    }

    public void signAgreements(List<SignReq> list, String ak, String sk) {
        var root = mapper.createObjectNode();
        var arr = root.putArray("sign_info_list");
        for (SignReq s : list) {
            var o = arr.addObject();
            o.put("agr_type", s.agrType());
            o.put("country", s.country());
            o.put("language", s.language());
            o.put("version", s.version());
        }
        call("POST", "/open-api-public/v1/agreements", "", root.toString(), ak, sk);
    }

    public record Devenv(String id, String name, String status) {}
    public record Conn(long connectionId, String status) {}
    public record Connections(long connectionId, List<Conn> list) {}
    public record ConnectionAddress(String url, String source) {}
    public record Agreement(String agrType, String country, String language, int signStatus, long version) {}
    public record SignReq(String agrType, String country, String language, long version) {}

    public static class DevStationException extends RuntimeException {
        public DevStationException(String message, Throwable cause) { super(message, cause); }
    }
}
