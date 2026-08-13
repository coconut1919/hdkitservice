package com.huaweicloud.hdkitservice.model;

public record ConnectResponse(String sessionId, String devStageId, String connectionId,
                              String connectionAddress, String status) {}
