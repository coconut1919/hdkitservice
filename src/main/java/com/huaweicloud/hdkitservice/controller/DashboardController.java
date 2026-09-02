package com.huaweicloud.hdkitservice.controller;

import com.huaweicloud.hdkitservice.model.AgentDistributionDTO;
import com.huaweicloud.hdkitservice.model.DeveloperSummaryDTO;
import com.huaweicloud.hdkitservice.model.DownloadSummaryDTO;
import com.huaweicloud.hdkitservice.model.DownloadTrendDTO;
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
}
