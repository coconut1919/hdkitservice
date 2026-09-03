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
@Table(name = "capability_daily_stats")
@IdClass(CapabilityDailyStats.PK.class)
public class CapabilityDailyStats {

    @Id
    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Id
    @Column(name = "capability", length = 32, nullable = false)
    private String capability;

    @Column(name = "call_count")
    private Long callCount;

    @Column(name = "user_count")
    private Long userCount;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public CapabilityDailyStats() {
    }

    public CapabilityDailyStats(LocalDate statDate, String capability, Long callCount, Long userCount) {
        this.statDate = statDate;
        this.capability = capability;
        this.callCount = callCount;
        this.userCount = userCount;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDate getStatDate() { return statDate; }
    public void setStatDate(LocalDate statDate) { this.statDate = statDate; }
    public String getCapability() { return capability; }
    public void setCapability(String capability) { this.capability = capability; }
    public Long getCallCount() { return callCount; }
    public void setCallCount(Long callCount) { this.callCount = callCount; }
    public Long getUserCount() { return userCount; }
    public void setUserCount(Long userCount) { this.userCount = userCount; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static class PK implements Serializable {
        private LocalDate statDate;
        private String capability;

        public PK() {
        }

        public PK(LocalDate statDate, String capability) {
            this.statDate = statDate;
            this.capability = capability;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PK pk)) return false;
            return Objects.equals(statDate, pk.statDate) && Objects.equals(capability, pk.capability);
        }

        @Override
        public int hashCode() {
            return Objects.hash(statDate, capability);
        }
    }
}
