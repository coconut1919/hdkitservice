package com.huaweicloud.hdkitservice.controller;

import com.huaweicloud.hdkitservice.model.AgentDistributionDTO;
import com.huaweicloud.hdkitservice.model.CapabilityDistributionDTO;
import com.huaweicloud.hdkitservice.model.CapabilitySummaryDTO;
import com.huaweicloud.hdkitservice.model.CapabilityTrendDTO;
import com.huaweicloud.hdkitservice.model.DeveloperSummaryDTO;
import com.huaweicloud.hdkitservice.model.DeveloperTrendDTO;
import com.huaweicloud.hdkitservice.model.DownloadSummaryDTO;
import com.huaweicloud.hdkitservice.model.DownloadTrendDTO;
import com.huaweicloud.hdkitservice.model.SkillRankingDTO;
import com.huaweicloud.hdkitservice.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/developer/server/hdkitservice/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/developer/summary")
    public DeveloperSummaryDTO developerSummary() {
        return dashboardService.getDeveloperSummary();
    }

    @GetMapping("/developer/trend")
    public DeveloperTrendDTO developerTrend() {
        return dashboardService.getDeveloperTrend();
    }

    @GetMapping("/agent/distribution")
    public AgentDistributionDTO agentDistribution() {
        return dashboardService.getAgentDistribution();
    }

    @GetMapping("/download/trend")
    public DownloadTrendDTO downloadTrend() {
        return dashboardService.getDownloadTrend();
    }

    @GetMapping("/download/summary")
    public DownloadSummaryDTO downloadSummary() {
        return dashboardService.getDownloadSummary();
    }

    @GetMapping("/aggregate")
    public String triggerAggregation() {
        dashboardService.aggregateMetrics(java.time.LocalDate.now());
        return "{\"status\":\"ok\"}";
    }

    // ==================== Open Capabilities ====================

    @GetMapping("/capability/summary")
    public CapabilitySummaryDTO capabilitySummary() {
        return dashboardService.getCapabilitySummary();
    }

    @GetMapping("/capability/trend")
    public CapabilityTrendDTO capabilityTrend() {
        return dashboardService.getCapabilityTrend();
    }

    @GetMapping("/capability/distribution")
    public CapabilityDistributionDTO capabilityDistribution() {
        return dashboardService.getCapabilityDistribution();
    }

    @GetMapping("/capability/skill/ranking")
    public SkillRankingDTO skillRanking() {
        return dashboardService.getSkillRanking();
    }
}
