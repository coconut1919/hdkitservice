package com.huaweicloud.hdkitservice.service;

import com.huaweicloud.hdkitservice.model.AgentDistributionDaily;
import com.huaweicloud.hdkitservice.model.AgentDistributionDTO;
import com.huaweicloud.hdkitservice.model.DeveloperSummaryDTO;
import com.huaweicloud.hdkitservice.model.DownloadSummaryDTO;
import com.huaweicloud.hdkitservice.model.DownloadTrendDTO;
import com.huaweicloud.hdkitservice.model.MetricDaily;
import com.huaweicloud.hdkitservice.model.NpmDownloadStats;
import com.huaweicloud.hdkitservice.repository.AgentDistributionDailyRepository;
import com.huaweicloud.hdkitservice.repository.MetricDailyRepository;
import com.huaweicloud.hdkitservice.repository.NpmDownloadStatsRepository;
import com.huaweicloud.hdkitservice.repository.TelemetryEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);
    private static final String KEY_TOTAL_DEVELOERS = "total_developers";
    private static final String KEY_DAU = "dau";
    private static final String KEY_MAU = "mau";
    private static final String KEY_NEW_USERS = "new_users";
    private static final String KEY_AGENT_TOTAL = "agent_total";
    private static final int TREND_DAYS = 30;

    private final MetricDailyRepository metricRepo;
    private final AgentDistributionDailyRepository agentRepo;
    private final NpmDownloadStatsRepository npmRepo;
    private final TelemetryEventRepository telemetryRepo;

    public DashboardService(MetricDailyRepository metricRepo,
                            AgentDistributionDailyRepository agentRepo,
                            NpmDownloadStatsRepository npmRepo,
                            TelemetryEventRepository telemetryRepo) {
        this.metricRepo = metricRepo;
        this.agentRepo = agentRepo;
        this.npmRepo = npmRepo;
        this.telemetryRepo = telemetryRepo;
    }

    public DeveloperSummaryDTO getDeveloperSummary() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate prevDay = today.minusDays(2);
        LocalDate monthAgo = today.minusDays(30);

        long totalDevs = getMetricValue(KEY_TOTAL_DEVELOERS, today, telemetryRepo::countDistinctUserHash);
        long dau = getMetricValue(KEY_DAU, today, () -> telemetryRepo.countDistinctUserHashByDate(today));
        long mau = getMetricValue(KEY_MAU, today, () -> telemetryRepo.countDistinctUserHashSince(monthAgo));
        long agentTotal = getMetricValue(KEY_AGENT_TOTAL, today, telemetryRepo::countDistinctInstallId);

        long newUsersToday = getMetricValue(KEY_NEW_USERS, today, () -> 0);
        long newUsersYesterday = getMetricValue(KEY_NEW_USERS, yesterday, () -> 0);
        double chainRatio = 0;
        if (newUsersYesterday > 0) {
            chainRatio = (double) (newUsersToday - newUsersYesterday) / newUsersYesterday * 100;
        } else if (newUsersToday > 0) {
            chainRatio = 100;
        }

        List<DeveloperSummaryDTO.DailyTrendPoint> dauTrend = buildDauTrend(today);

        return new DeveloperSummaryDTO(totalDevs, newUsersToday, chainRatio, dau, mau, agentTotal, dauTrend);
    }

    public AgentDistributionDTO getAgentDistribution() {
        List<AgentDistributionDaily> latest = agentRepo.findLatestDistribution();

        if (latest == null || latest.isEmpty()) {
            return buildAgentDistributionFromTelemetry();
        }

        long total = latest.stream().mapToLong(AgentDistributionDaily::getInstallCount).sum();
        List<AgentDistributionDTO.AgentItem> items = new ArrayList<>();
        for (AgentDistributionDaily d : latest) {
            String name = normalizeAgentName(d.getAgentName());
            double pct = total > 0 ? (double) d.getInstallCount() / total * 100 : 0;
            items.add(new AgentDistributionDTO.AgentItem(name, d.getInstallCount(), Math.round(pct * 10) / 10.0));
        }

        items = mergeAgentItems(items);
        total = items.stream().mapToLong(AgentDistributionDTO.AgentItem::count).sum();

        String dateStr = latest.isEmpty() ? LocalDate.now().toString() : latest.get(0).getMetricDate().toString();
        return new AgentDistributionDTO(dateStr, total, items);
    }

    public DownloadTrendDTO getDownloadTrend() {
        LocalDate startDate = LocalDate.now().minusDays(TREND_DAYS);
        List<NpmDownloadStats> stats = npmRepo.findSince(startDate);

        List<DownloadTrendDTO.TrendPoint> npmDaily = new ArrayList<>();
        long total = 0;
        for (NpmDownloadStats s : stats) {
            npmDaily.add(new DownloadTrendDTO.TrendPoint(s.getStatDate().toString(), s.getDailyDownloads()));
            total += s.getDailyDownloads();
        }

        return new DownloadTrendDTO(npmDaily, total);
    }

    public DownloadSummaryDTO getDownloadSummary() {
        Optional<NpmDownloadStats> latestOpt = npmRepo.findLatest();
        if (latestOpt.isEmpty()) {
            return new DownloadSummaryDTO(0, 0, 0, "", "");
        }
        NpmDownloadStats latest = latestOpt.get();
        return new DownloadSummaryDTO(
                latest.getDailyDownloads() != null ? latest.getDailyDownloads() : 0,
                latest.getWeekDownloads() != null ? latest.getWeekDownloads() : 0,
                latest.getCumulativeDownloads() != null ? latest.getCumulativeDownloads() : 0,
                latest.getPackageName(),
                latest.getStatDate().toString()
        );
    }

    public void aggregateMetrics(LocalDate date) {
        log.info("[dashboard] aggregating metrics for {}", date);

        saveMetric(date, KEY_TOTAL_DEVELOERS, telemetryRepo.countDistinctUserHash());
        saveMetric(date, KEY_DAU, telemetryRepo.countDistinctUserHashByDate(date));
        saveMetric(date, KEY_MAU, telemetryRepo.countDistinctUserHashSince(date.minusDays(30)));
        saveMetric(date, KEY_AGENT_TOTAL, telemetryRepo.countDistinctInstallId());

        aggregateAgentDistribution(date);
    }

    private void aggregateAgentDistribution(LocalDate date) {
        List<Object[]> rows = telemetryRepo.agentDistribution();
        for (Object[] row : rows) {
            String rawName = (String) row[0];
            Number count = (Number) row[1];
            String normalName = normalizeAgentName(rawName);

            AgentDistributionDaily existing = agentRepo
                    .findByMetricDateOrderByInstallCountDesc(date)
                    .stream()
                    .filter(a -> a.getAgentName().equals(normalName))
                    .findFirst()
                    .orElse(null);

            if (existing != null) {
                existing.setInstallCount(count.intValue());
                existing.setUpdatedAt(java.time.LocalDateTime.now());
                agentRepo.save(existing);
            } else {
                agentRepo.save(new AgentDistributionDaily(date, normalName, count.intValue()));
            }
        }
        log.info("[dashboard] agent distribution aggregated for {}, {} agents", date, rows.size());
    }

    private void saveMetric(LocalDate date, String key, long value) {
        Optional<MetricDaily> existing = metricRepo.findByMetricDateAndMetricKey(date, key);
        if (existing.isPresent()) {
            MetricDaily m = existing.get();
            m.setMetricValue(value);
            m.setUpdatedAt(java.time.LocalDateTime.now());
            metricRepo.save(m);
        } else {
            metricRepo.save(new MetricDaily(date, key, value, null));
        }
    }

    private long getMetricValue(String key, LocalDate date, java.util.function.LongSupplier fallback) {
        Optional<MetricDaily> metric = metricRepo.findByMetricDateAndMetricKey(date, key);
        if (metric.isPresent() && metric.get().getMetricValue() != null) {
            return metric.get().getMetricValue();
        }
        try {
            return fallback.getAsLong();
        } catch (Exception e) {
            log.debug("[dashboard] fallback query failed for {}: {}", key, e.getMessage());
            return 0;
        }
    }

    private List<DeveloperSummaryDTO.DailyTrendPoint> buildDauTrend(LocalDate today) {
        LocalDate startDate = today.minusDays(TREND_DAYS - 1);
        List<MetricDaily> metrics = metricRepo.findByKeySince(KEY_DAU, startDate);

        if (!metrics.isEmpty()) {
            return metrics.stream()
                    .map(m -> new DeveloperSummaryDTO.DailyTrendPoint(m.getMetricDate().toString(), m.getMetricValue()))
                    .toList();
        }

        List<Object[]> rows = telemetryRepo.dailyActiveUsersSince(startDate);
        List<DeveloperSummaryDTO.DailyTrendPoint> points = new ArrayList<>();
        for (Object[] row : rows) {
            java.sql.Date sqlDate = (java.sql.Date) row[0];
            long cnt = ((Number) row[1]).longValue();
            points.add(new DeveloperSummaryDTO.DailyTrendPoint(sqlDate.toLocalDate().toString(), cnt));
        }
        return points;
    }

    private AgentDistributionDTO buildAgentDistributionFromTelemetry() {
        List<Object[]> rows = telemetryRepo.agentDistribution();
        long total = 0;
        List<AgentDistributionDTO.AgentItem> items = new ArrayList<>();
        for (Object[] row : rows) {
            String name = normalizeAgentName((String) row[0]);
            int count = ((Number) row[1]).intValue();
            total += count;
            items.add(new AgentDistributionDTO.AgentItem(name, count, 0));
        }
        items = mergeAgentItems(items);
        total = items.stream().mapToLong(AgentDistributionDTO.AgentItem::count).sum();
        for (int i = 0; i < items.size(); i++) {
            AgentDistributionDTO.AgentItem item = items.get(i);
            double pct = total > 0 ? (double) item.count() / total * 100 : 0;
            items.set(i, new AgentDistributionDTO.AgentItem(item.name(), item.count(), Math.round(pct * 10) / 10.0));
        }
        return new AgentDistributionDTO(LocalDate.now().toString(), total, items);
    }

    private List<AgentDistributionDTO.AgentItem> mergeAgentItems(List<AgentDistributionDTO.AgentItem> items) {
        Map<String, Integer> merged = new HashMap<>();
        for (AgentDistributionDTO.AgentItem item : items) {
            merged.merge(item.name(), item.count(), Integer::sum);
        }
        List<AgentDistributionDTO.AgentItem> result = new ArrayList<>();
        merged.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .forEach(e -> result.add(new AgentDistributionDTO.AgentItem(e.getKey(), e.getValue(), 0)));
        return result;
    }

    static String normalizeAgentName(String raw) {
        if (raw == null || raw.isBlank()) {
            return "unknown";
        }
        String lower = raw.toLowerCase().trim();
        if (lower.startsWith("opencode")) return "opencode";
        if (lower.startsWith("codex")) return "codex";
        if (lower.startsWith("workbuddy")) return "workbuddy";
        if (lower.startsWith("码道")) return "码道";
        if (lower.startsWith("officeace")) return "officeace";
        return raw.trim();
    }
}
