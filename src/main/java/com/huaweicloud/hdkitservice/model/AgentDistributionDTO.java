package com.huaweicloud.hdkitservice.model;

import java.util.List;

public record AgentDistributionDTO(
        String date,
        long totalInstallIds,
        List<AgentItem> agents
) {
    public record AgentItem(String name, int count, double percentage) {}
}
