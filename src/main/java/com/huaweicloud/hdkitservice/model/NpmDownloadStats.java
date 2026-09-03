package com.huaweicloud.hdkitservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "npm_download_stats")
public class NpmDownloadStats {

    @Id
    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "package_name", length = 128, nullable = false)
    private String packageName;

    @Column(name = "daily_downloads")
    private Long dailyDownloads;

    @Column(name = "week_downloads")
    private Long weekDownloads;

    @Column(name = "cumulative_downloads")
    private Long cumulativeDownloads;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public NpmDownloadStats() {
    }

    public NpmDownloadStats(LocalDate statDate, String packageName, Long dailyDownloads,
                            Long weekDownloads, Long cumulativeDownloads) {
        this.statDate = statDate;
        this.packageName = packageName;
        this.dailyDownloads = dailyDownloads;
        this.weekDownloads = weekDownloads;
        this.cumulativeDownloads = cumulativeDownloads;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDate getStatDate() { return statDate; }
    public void setStatDate(LocalDate statDate) { this.statDate = statDate; }
    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }
    public Long getDailyDownloads() { return dailyDownloads; }
    public void setDailyDownloads(Long dailyDownloads) { this.dailyDownloads = dailyDownloads; }
    public Long getWeekDownloads() { return weekDownloads; }
    public void setWeekDownloads(Long weekDownloads) { this.weekDownloads = weekDownloads; }
    public Long getCumulativeDownloads() { return cumulativeDownloads; }
    public void setCumulativeDownloads(Long cumulativeDownloads) { this.cumulativeDownloads = cumulativeDownloads; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
