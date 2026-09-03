package com.huaweicloud.hdkitservice.model;

public record CapabilitySummaryDTO(
        long totalCalls,
        long uniqueUsers,
        long dailyAvgCalls,
        long todayCalls
) {}
