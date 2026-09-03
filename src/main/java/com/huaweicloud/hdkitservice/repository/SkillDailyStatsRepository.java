package com.huaweicloud.hdkitservice.repository;

import com.huaweicloud.hdkitservice.model.SkillDailyStats;
import com.huaweicloud.hdkitservice.model.SkillDailyStats.PK;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SkillDailyStatsRepository extends JpaRepository<SkillDailyStats, PK> {

    @Query("SELECT s FROM SkillDailyStats s WHERE s.statDate >= :startDate ORDER BY s.callCount DESC")
    List<SkillDailyStats> findTopSkillsSince(@Param("startDate") LocalDate startDate);
}
