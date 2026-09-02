package com.huaweicloud.hdkitservice.model;

public record DownloadSummaryDTO(
        long npmToday,
        long npmWeek,
        long npmCumulative,
        String packageName,
        String latestDate
) {}
