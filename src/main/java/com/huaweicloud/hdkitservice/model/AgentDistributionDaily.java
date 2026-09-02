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
@Table(name = "agent_distribution_daily")
@IdClass(AgentDistributionDaily.PK.class)
public class AgentDistributionDaily {

    @Id
    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;

    @Id
    @Column(name = "agent_name", length = 64, nullable = false)
    private String agentName;

    @Column(name = "install_count")
    private Integer installCount;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public AgentDistributionDaily() {
    }

    public AgentDistributionDaily(LocalDate metricDate, String agentName, Integer installCount) {
        this.metricDate = metricDate;
        this.agentName = agentName;
        this.installCount = installCount;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDate getMetricDate() { return metricDate; }
    public void setMetricDate(LocalDate metricDate) { this.metricDate = metricDate; }
    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }
    public Integer getInstallCount() { return installCount; }
    public void setInstallCount(Integer installCount) { this.installCount = installCount; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static class PK implements Serializable {
        private LocalDate metricDate;
        private String agentName;

        public PK() {
        }

        public PK(LocalDate metricDate, String agentName) {
            this.metricDate = metricDate;
            this.agentName = agentName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PK pk)) return false;
            return Objects.equals(metricDate, pk.metricDate) && Objects.equals(agentName, pk.agentName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(metricDate, agentName);
        }
    }
}
