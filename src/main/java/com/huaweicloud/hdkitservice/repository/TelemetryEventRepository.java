package com.huaweicloud.hdkitservice.repository;

import com.huaweicloud.hdkitservice.model.TelemetryEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TelemetryEventRepository extends JpaRepository<TelemetryEvent, String> {

    @Query(value = "SELECT COUNT(DISTINCT e.user_hash) FROM telemetry_event e " +
            "WHERE e.user_hash IS NOT NULL AND e.user_hash <> ''",
            nativeQuery = true)
    long countDistinctUserHash();

    @Query(value = "SELECT COUNT(DISTINCT e.user_hash) FROM telemetry_event e " +
            "WHERE e.user_hash IS NOT NULL AND e.user_hash <> '' " +
            "AND DATE(e.server_time) = :date",
            nativeQuery = true)
    long countDistinctUserHashByDate(@Param("date") LocalDate date);

    @Query(value = "SELECT COUNT(DISTINCT e.user_hash) FROM telemetry_event e " +
            "WHERE e.user_hash IS NOT NULL AND e.user_hash <> '' " +
            "AND DATE(e.server_time) >= :startDate",
            nativeQuery = true)
    long countDistinctUserHashSince(@Param("startDate") LocalDate startDate);

    @Query(value = "SELECT COUNT(DISTINCT e.user_hash) FROM telemetry_event e " +
            "WHERE e.user_hash IS NOT NULL AND e.user_hash <> '' " +
            "AND DATE(e.server_time) BETWEEN :prevStart AND :prevEnd",
            nativeQuery = true)
    long countDistinctUserHashBetween(@Param("prevStart") LocalDate prevStart,
                                      @Param("prevEnd") LocalDate prevEnd);

    @Query(value = "SELECT COUNT(DISTINCT e.install_id) FROM telemetry_event e " +
            "WHERE e.install_id IS NOT NULL AND e.install_id <> ''",
            nativeQuery = true)
    long countDistinctInstallId();

    @Query(value = "SELECT DATE(e.server_time) AS d, COUNT(DISTINCT e.user_hash) AS cnt " +
            "FROM telemetry_event e " +
            "WHERE e.user_hash IS NOT NULL AND e.user_hash <> '' " +
            "AND DATE(e.server_time) >= :startDate " +
            "GROUP BY DATE(e.server_time) " +
            "ORDER BY d",
            nativeQuery = true)
    List<Object[]> dailyActiveUsersSince(@Param("startDate") LocalDate startDate);

    @Query(value = "SELECT COALESCE(e.agent_harness, 'unknown') AS agent, " +
            "COUNT(DISTINCT e.install_id) AS cnt " +
            "FROM telemetry_event e " +
            "WHERE e.install_id IS NOT NULL AND e.install_id <> '' " +
            "GROUP BY COALESCE(e.agent_harness, 'unknown') " +
            "ORDER BY cnt DESC",
            nativeQuery = true)
    List<Object[]> agentDistribution();
}
