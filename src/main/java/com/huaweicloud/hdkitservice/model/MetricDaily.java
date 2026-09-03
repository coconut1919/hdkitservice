package com.huaweicloud.hdkitservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "metric_daily")
@IdClass(MetricDaily.PK.class)
public class MetricDaily {

    @Id
    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;

    @Id
    @Column(name = "metric_key", length = 64, nullable = false)
    private String metricKey;

    @Column(name = "metric_value")
    private Long metricValue;

    @Column(name = "metric_value_2")
    private Long metricValue2;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public MetricDaily() {
    }

    public MetricDaily(LocalDate metricDate, String metricKey, Long metricValue, Long metricValue2) {
        this.metricDate = metricDate;
        this.metricKey = metricKey;
        this.metricValue = metricValue;
        this.metricValue2 = metricValue2;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDate getMetricDate() { return metricDate; }
    public void setMetricDate(LocalDate metricDate) { this.metricDate = metricDate; }
    public String getMetricKey() { return metricKey; }
    public void setMetricKey(String metricKey) { this.metricKey = metricKey; }
    public Long getMetricValue() { return metricValue; }
    public void setMetricValue(Long metricValue) { this.metricValue = metricValue; }
    public Long getMetricValue2() { return metricValue2; }
    public void setMetricValue2(Long metricValue2) { this.metricValue2 = metricValue2; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static class PK implements Serializable {
        private LocalDate metricDate;
        private String metricKey;

        public PK() {
        }

        public PK(LocalDate metricDate, String metricKey) {
            this.metricDate = metricDate;
            this.metricKey = metricKey;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PK pk)) return false;
            return Objects.equals(metricDate, pk.metricDate) && Objects.equals(metricKey, pk.metricKey);
        }

        @Override
        public int hashCode() {
            return Objects.hash(metricDate, metricKey);
        }
    }
}
