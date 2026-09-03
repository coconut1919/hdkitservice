package com.huaweicloud.hdkitservice.service;

import com.huaweicloud.hdkitservice.model.AgentDistributionDTO;
import com.huaweicloud.hdkitservice.model.DeveloperSummaryDTO;
import com.huaweicloud.hdkitservice.model.DownloadSummaryDTO;
import com.huaweicloud.hdkitservice.model.MetricDaily;
import com.huaweicloud.hdkitservice.model.NpmDownloadStats;
import com.huaweicloud.hdkitservice.repository.AgentDistributionDailyRepository;
import com.huaweicloud.hdkitservice.repository.CapabilityDailyStatsRepository;
import com.huaweicloud.hdkitservice.repository.MetricDailyRepository;
import com.huaweicloud.hdkitservice.repository.SkillDailyStatsRepository;
import com.huaweicloud.hdkitservice.repository.NpmDownloadStatsRepository;
import com.huaweicloud.hdkitservice.repository.TelemetryEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private MetricDailyRepository metricRepo;
    @Mock
    private AgentDistributionDailyRepository agentRepo;
    @Mock
    private NpmDownloadStatsRepository npmRepo;
    @Mock
    private TelemetryEventRepository telemetryRepo;
    @Mock
    private CapabilityDailyStatsRepository capabilityDailyRepo;
    @Mock
    private SkillDailyStatsRepository skillDailyRepo;

    @InjectMocks
    private DashboardService service;

    @Test
    void getDeveloperSummaryWithPreAggregatedData() {
        LocalDate today = LocalDate.now();
        when(metricRepo.findByMetricDateAndMetricKey(eq(today), anyString()))
                .thenAnswer(inv -> {
                    String key = inv.getArgument(1);
                    long val = switch (key) {
                        case "total_developers" -> 1000L;
                        case "dau" -> 200L;
                        case "mau" -> 800L;
                        case "agent_total" -> 15L;
                        case "new_users" -> 50L;
                        default -> 0L;
                    };
                    return Optional.of(new MetricDaily(today, key, val, null));
                });
        when(metricRepo.findByKeySince(anyString(), any()))
                .thenReturn(List.of(new MetricDaily(today, "dau", 200L, null)));

        DeveloperSummaryDTO dto = service.getDeveloperSummary();

        assertEquals(1000, dto.totalDevelopers());
        assertEquals(200, dto.dau());
        assertEquals(800, dto.mau());
        assertEquals(15, dto.agentTotal());
        assertEquals(50, dto.newUsersToday());
    }

    @Test
    void getDeveloperSummaryFallbackToTelemetry() {
        when(metricRepo.findByMetricDateAndMetricKey(any(), anyString()))
                .thenReturn(Optional.empty());
        when(metricRepo.findByKeySince(anyString(), any()))
                .thenReturn(List.of());
        when(telemetryRepo.countDistinctUserHash()).thenReturn(5000L);
        when(telemetryRepo.countDistinctUserHashByDate(any())).thenReturn(300L);
        when(telemetryRepo.countDistinctUserHashSince(any())).thenReturn(2000L);
        when(telemetryRepo.countDistinctAgentHarness()).thenReturn(42L);
        when(telemetryRepo.dailyActiveUsersSince(any()))
                .thenReturn(List.<Object[]>of());

        DeveloperSummaryDTO dto = service.getDeveloperSummary();

        assertEquals(5000, dto.totalDevelopers());
        assertEquals(300, dto.dau());
        assertEquals(2000, dto.mau());
        assertEquals(42, dto.agentTotal());
    }

    @Test
    void getAgentDistributionFromTelemetry() {
        when(agentRepo.findLatestDistribution()).thenReturn(List.of());
        when(telemetryRepo.agentDistribution()).thenReturn(List.<Object[]>of(
                new Object[]{"opencode linux", 500},
                new Object[]{"opencode windows", 300},
                new Object[]{"codex windows", 200}
        ));

        AgentDistributionDTO dto = service.getAgentDistribution();

        assertEquals(2, dto.agents().size());
        AgentDistributionDTO.AgentItem first = dto.agents().get(0);
        assertEquals("opencode", first.name());
        assertEquals(800, first.count());
    }

    @Test
    void getDownloadSummaryWithNoData() {
        when(npmRepo.findLatest()).thenReturn(Optional.empty());

        DownloadSummaryDTO dto = service.getDownloadSummary();

        assertEquals(0, dto.npmToday());
        assertEquals(0, dto.npmCumulative());
    }

    @Test
    void getDownloadSummaryWithData() {
        LocalDate date = LocalDate.of(2026, 9, 1);
        NpmDownloadStats stats = new NpmDownloadStats(date, "@huaweicloud/huaweicloud-devkit", 478L, 2322L, 10410L);
        when(npmRepo.findLatest()).thenReturn(Optional.of(stats));

        DownloadSummaryDTO dto = service.getDownloadSummary();

        assertEquals(478, dto.npmToday());
        assertEquals(2322, dto.npmWeek());
        assertEquals(10410, dto.npmCumulative());
    }

    @Test
    void normalizeAgentName() {
        assertEquals("opencode", DashboardService.normalizeAgentName("opencode linux"));
        assertEquals("opencode", DashboardService.normalizeAgentName("opencode windows"));
        assertEquals("codex", DashboardService.normalizeAgentName("codex windows"));
        assertEquals("workbuddy", DashboardService.normalizeAgentName("workbuddy windows"));
        assertEquals("码道", DashboardService.normalizeAgentName("码道 IDE windows"));
        assertEquals("码道", DashboardService.normalizeAgentName("码道 CLI"));
        assertEquals("码道", DashboardService.normalizeAgentName("码道 work windows"));
        assertEquals("officeace", DashboardService.normalizeAgentName("officeace windows"));
        assertEquals("DSH", DashboardService.normalizeAgentName("DSH"));
        assertEquals("unknown", DashboardService.normalizeAgentName(null));
        assertEquals("unknown", DashboardService.normalizeAgentName(""));
    }
}
