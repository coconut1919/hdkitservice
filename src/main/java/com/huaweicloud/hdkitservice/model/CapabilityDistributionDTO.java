package com.huaweicloud.hdkitservice.model;

import java.util.List;

public record CapabilityDistributionDTO(
        List<CapabilityItem> capabilities
) {
    public record CapabilityItem(
            String capability,
            long callCount,
            double percentage
    ) {}
}
