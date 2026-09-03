package com.huaweicloud.hdkitservice.model;

import java.util.List;

public record CapabilityTrendDTO(
        List<String> dates,
        List<CapabilityTrendLine> lines
) {
    public record CapabilityTrendLine(
            String capability,
            List<long[]> data
    ) {}
}
