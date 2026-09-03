package com.huaweicloud.hdkitservice.repository;

import com.huaweicloud.hdkitservice.model.AgentDistributionDaily;
import com.huaweicloud.hdkitservice.model.AgentDistributionDaily.PK;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AgentDistributionDailyRepository extends JpaRepository<AgentDistributionDaily, PK> {

    List<AgentDistributionDaily> findByMetricDateOrderByInstallCountDesc(LocalDate metricDate);

    @Query("SELECT a FROM AgentDistributionDaily a WHERE a.metricDate = " +
            "(SELECT MAX(x.metricDate) FROM AgentDistributionDaily x) " +
            "ORDER BY a.installCount DESC")
    List<AgentDistributionDaily> findLatestDistribution();
}
