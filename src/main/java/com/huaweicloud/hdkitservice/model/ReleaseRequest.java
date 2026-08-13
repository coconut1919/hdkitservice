package com.huaweicloud.hdkitservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ReleaseRequest(
        @JsonProperty("session_id") String sessionId,
        @JsonProperty("dev_stage_id") String devStageId) {}
