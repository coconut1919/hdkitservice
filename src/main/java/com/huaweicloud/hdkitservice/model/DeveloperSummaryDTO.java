package com.huaweicloud.hdkitservice.model;

import java.util.List;

public record DeveloperSummaryDTO(
        long totalDevelopers,
        long newUsersToday,
        double newUsersChainRatio,
        long dau,
        long mau,
        long agentTotal,
        List<DailyTrendPoint> dauTrend
) {
    public record DailyTrendPoint(String date, long value) {}
}
