package com.huaweicloud.hdkitservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TelemetryEventDto(
        @JsonProperty("key") String key,
        @JsonProperty("value") String value,
        String capability,
        String installId,
        String userHash,
        String version,
        String harness,
        String agentVersion,
        String os,
        String osVersion
) {}