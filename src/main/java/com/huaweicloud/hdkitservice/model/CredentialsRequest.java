package com.huaweicloud.hdkitservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CredentialsRequest(
        @JsonProperty("session_id") String sessionId,
        @JsonProperty("dev_stage_id") String devStageId,
        @JsonProperty("enable_sts") Boolean enableSts) {}
