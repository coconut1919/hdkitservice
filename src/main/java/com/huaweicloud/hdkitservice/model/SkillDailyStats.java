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
@Table(name = "skill_daily_stats")
@IdClass(SkillDailyStats.PK.class)
public class SkillDailyStats {

    @Id
    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Id
    @Column(name = "skill_name", length = 128, nullable = false)
    private String skillName;

    @Column(name = "call_count")
    private Long callCount;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public SkillDailyStats() {
    }

    public SkillDailyStats(LocalDate statDate, String skillName, Long callCount) {
        this.statDate = statDate;
        this.skillName = skillName;
        this.callCount = callCount;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDate getStatDate() { return statDate; }
    public void setStatDate(LocalDate statDate) { this.statDate = statDate; }
    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }
    public Long getCallCount() { return callCount; }
    public void setCallCount(Long callCount) { this.callCount = callCount; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static class PK implements Serializable {
        private LocalDate statDate;
        private String skillName;

        public PK() {
        }

        public PK(LocalDate statDate, String skillName) {
            this.statDate = statDate;
            this.skillName = skillName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PK pk)) return false;
            return Objects.equals(statDate, pk.statDate) && Objects.equals(skillName, pk.skillName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(statDate, skillName);
        }
    }
}
