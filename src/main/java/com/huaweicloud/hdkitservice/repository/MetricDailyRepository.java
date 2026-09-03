package com.huaweicloud.hdkitservice.repository;

import com.huaweicloud.hdkitservice.model.MetricDaily;
import com.huaweicloud.hdkitservice.model.MetricDaily.PK;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MetricDailyRepository extends JpaRepository<MetricDaily, PK> {

    Optional<MetricDaily> findByMetricDateAndMetricKey(LocalDate metricDate, String metricKey);

    @Query("SELECT m FROM MetricDaily m WHERE m.metricKey = :key AND m.metricDate >= :startDate ORDER BY m.metricDate")
    List<MetricDaily> findByKeySince(@Param("key") String key, @Param("startDate") LocalDate startDate);

    @Query("SELECT m FROM MetricDaily m WHERE m.metricKey = :key ORDER BY m.metricDate DESC LIMIT 1")
    Optional<MetricDaily> findLatestByKey(@Param("key") String key);
}
