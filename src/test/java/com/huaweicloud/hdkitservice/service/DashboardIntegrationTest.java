package com.huaweicloud.hdkitservice.service;

import com.huaweicloud.hdkitservice.model.AgentDistributionDTO;
import com.huaweicloud.hdkitservice.model.DeveloperSummaryDTO;
import com.huaweicloud.hdkitservice.model.DownloadSummaryDTO;
import com.huaweicloud.hdkitservice.model.NpmDownloadStats;
import com.huaweicloud.hdkitservice.model.TelemetryEvent;
import com.huaweicloud.hdkitservice.repository.NpmDownloadStatsRepository;
import com.huaweicloud.hdkitservice.repository.TelemetryEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class DashboardIntegrationTest {

    @Autowired
    private DashboardService dashboardService;
    @Autowired
    private TelemetryEventRepository telemetryRepo;
    @Autowired
    private NpmDownloadStatsRepository npmRepo;

    @BeforeEach
    void setUp() {
        telemetryRepo.deleteAll();
        npmRepo.deleteAll();

        LocalDateTime now = LocalDateTime.now();
        String[] agents = {"opencode linux", "opencode windows", "codex windows", "码道 IDE windows"};
        for (int i = 0; i < 10; i++) {
            telemetryRepo.save(new TelemetryEvent(
                    UUID.randomUUID().toString(),
                    "event_" + i,
                    "value_" + i,
                    "install_" + (i % 4),
                    "user_hash_" + (i % 5),
                    "v2.4.1",
                    agents[i % agents.length],
                    "1.0",
                    "Linux",
                    "Ubuntu 22.04",
                    "skill",
                    System.currentTimeMillis(),
                    now
            ));
        }
    }

    @Test
    void developerSummaryWithTelemetryFallback() {
        DeveloperSummaryDTO dto = dashboardService.getDeveloperSummary();

        assertTrue(dto.totalDevelopers() > 0, "totalDevelopers should be > 0");
        assertTrue(dto.dau() > 0, "dau should be > 0");
        assertTrue(dto.mau() > 0, "mau should be > 0");
        assertTrue(dto.agentTotal() > 0, "agentTotal should be > 0");
        assertFalse(dto.dauTrend().isEmpty(), "dauTrend should not be empty");
    }

    @Test
    void agentDistributionMergesPlatforms() {
        AgentDistributionDTO dto = dashboardService.getAgentDistribution();

        assertFalse(dto.agents().isEmpty(), "agents should not be empty");
        boolean hasOpencode = dto.agents().stream()
                .anyMatch(a -> a.name().equals("opencode"));
        assertTrue(hasOpencode, "should have merged 'opencode' agent");

        boolean hasMerged = dto.agents().stream()
                .filter(a -> a.name().equals("opencode"))
                .count() == 1;
        assertTrue(hasMerged, "opencode should appear only once (merged)");
    }

    @Test
    void downloadSummaryWithNpmData() {
        LocalDate date = LocalDate.now().minusDays(1);
        npmRepo.save(new NpmDownloadStats(date, "@huaweicloud/huaweicloud-devkit", 478L, 2322L, 10410L));

        DownloadSummaryDTO dto = dashboardService.getDownloadSummary();

        assertEquals(478, dto.npmToday());
        assertEquals(2322, dto.npmWeek());
        assertEquals(10410, dto.npmCumulative());
    }

    @Test
    void downloadSummaryWithNoData() {
        DownloadSummaryDTO dto = dashboardService.getDownloadSummary();

        assertEquals(0, dto.npmToday());
        assertEquals(0, dto.npmCumulative());
    }

    @Test
    void aggregateMetricsWorks() {
        LocalDate today = LocalDate.now();
        dashboardService.aggregateMetrics(today);

        DeveloperSummaryDTO dto = dashboardService.getDeveloperSummary();
        assertTrue(dto.totalDevelopers() > 0, "after aggregation, totalDevelopers should be > 0");
    }
}
