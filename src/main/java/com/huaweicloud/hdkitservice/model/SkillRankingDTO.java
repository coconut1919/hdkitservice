package com.huaweicloud.hdkitservice.model;

import java.util.List;

public record SkillRankingDTO(
        List<SkillItem> skills
) {
    public record SkillItem(
            int rank,
            String skillName,
            long callCount,
            double percentage
    ) {}
}
