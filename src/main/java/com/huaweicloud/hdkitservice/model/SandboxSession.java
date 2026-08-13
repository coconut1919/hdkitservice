package com.huaweicloud.hdkitservice.model;

public record SandboxSession(String sessionId, String name, String devStageId, String connectionId,
                             String address, String status, long createdAt, long updatedAt) {}
