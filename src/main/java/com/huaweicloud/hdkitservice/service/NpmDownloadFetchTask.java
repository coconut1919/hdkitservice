package com.huaweicloud.hdkitservice.service;

import com.huaweicloud.hdkitservice.config.DashboardConfig;
import com.huaweicloud.hdkitservice.model.NpmDownloadStats;
import com.huaweicloud.hdkitservice.repository.NpmDownloadStatsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Component
public class NpmDownloadFetchTask {

    private static final Logger log = LoggerFactory.getLogger(NpmDownloadFetchTask.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final NpmDownloadClient npmClient;
    private final NpmDownloadStatsRepository npmRepo;
    private final DashboardConfig config;

    public NpmDownloadFetchTask(NpmDownloadClient npmClient,
                                 NpmDownloadStatsRepository npmRepo,
                                 DashboardConfig config) {
        this.npmClient = npmClient;
        this.npmRepo = npmRepo;
        this.config = config;
    }

    @Scheduled(cron = "0 15 6 * * *")
    public void fetch() {
        if (!config.npmFetchEnabled()) {
            return;
        }
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("[npm-fetch] start for {}", yesterday);

        try {
            long daily = npmClient.fetchDailyDownloads(yesterday);
            long week = npmClient.fetchWeekDownloads();

            LocalDate publishDate = LocalDate.parse(config.npmPublishDate(), FMT);
            long cumulative = npmClient.fetchCumulativeDownloads(publishDate, yesterday);

            Optional<NpmDownloadStats> existing = npmRepo.findByStatDate(yesterday);
            NpmDownloadStats stats;
            if (existing.isPresent()) {
                stats = existing.get();
                stats.setDailyDownloads(daily);
                stats.setWeekDownloads(week);
                stats.setCumulativeDownloads(cumulative);
                stats.setUpdatedAt(java.time.LocalDateTime.now());
            } else {
                stats = new NpmDownloadStats(yesterday, config.npmPackageName(),
                        daily, week, cumulative);
            }
            npmRepo.save(stats);
            log.info("[npm-fetch] done: daily={}, week={}, cumulative={}", daily, week, cumulative);
        } catch (Exception e) {
            log.error("[npm-fetch] failed: {}", e.getMessage(), e);
        }
    }
}
