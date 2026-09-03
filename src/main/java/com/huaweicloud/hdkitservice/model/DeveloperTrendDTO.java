package com.huaweicloud.hdkitservice.model;

import java.util.List;

public record DeveloperTrendDTO(List<TrendPoint> points) {
    public record TrendPoint(String date, long dau, long mau) {}
}
