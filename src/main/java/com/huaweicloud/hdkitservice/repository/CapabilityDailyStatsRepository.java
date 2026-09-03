package com.huaweicloud.hdkitservice.repository;

import com.huaweicloud.hdkitservice.model.CapabilityDailyStats;
import com.huaweicloud.hdkitservice.model.CapabilityDailyStats.PK;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CapabilityDailyStatsRepository extends JpaRepository<CapabilityDailyStats, PK> {

    List<CapabilityDailyStats> findByStatDateGreaterThanEqualOrderByStatDateAscCapability(LocalDate startDate);

    List<CapabilityDailyStats> findByStatDateOrderByCallCountDesc(LocalDate statDate);
}
