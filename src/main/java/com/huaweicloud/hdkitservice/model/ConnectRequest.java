package com.huaweicloud.hdkitservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public record ConnectRequest(
        @JsonProperty("name") String name,
        @JsonProperty("template_id") String templateId,
        @JsonProperty("flavor_id") String flavorId,
        @JsonProperty("source") String source,
        @JsonProperty("env") Map<String, String> env,
        @JsonProperty("git") Map<String, String> git) {}
