package com.huaweicloud.hdkitservice.service;

import com.huaweicloud.hdkitservice.config.DashboardConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class MetricAggregationTask {

    private static final Logger log = LoggerFactory.getLogger(MetricAggregationTask.class);

    private final DashboardService dashboardService;
    private final DashboardConfig config;

    public MetricAggregationTask(DashboardService dashboardService, DashboardConfig config) {
        this.dashboardService = dashboardService;
        this.config = config;
    }

    @Scheduled(cron = "0 30 1 * * *")
    public void aggregate() {
        if (!config.aggregationEnabled()) {
            return;
        }
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("[metric-aggregation] start for {}", yesterday);
        try {
            dashboardService.aggregateMetrics(yesterday);
            dashboardService.aggregateMetrics(LocalDate.now());

            dashboardService.aggregateCapabilityMetrics(yesterday);
            dashboardService.aggregateCapabilityMetrics(LocalDate.now());

            log.info("[metric-aggregation] done");
        } catch (Exception e) {
            log.error("[metric-aggregation] failed: {}", e.getMessage(), e);
        }
    }
}
