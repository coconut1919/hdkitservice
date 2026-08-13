package com.huaweicloud.hdkitservice.model;

public record SandboxSession(String sessionId, String userKey, String name, String devStageId, String connectionId,
                             String address, String status, long createdAt, long updatedAt) {}
