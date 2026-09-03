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

    @Query(value = "SELECT COUNT(DISTINCT CONCAT(e.install_id, '_', COALESCE(e.agent_harness, 'unknown'))) " +
            "FROM telemetry_event e WHERE e.install_id IS NOT NULL AND e.install_id <> ''",
            nativeQuery = true)
    long countDistinctAgentHarness();

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

    // ==================== Open Capabilities ====================

    @Query(value = "SELECT COUNT(*) FROM telemetry_event e " +
            "WHERE e.capability IS NOT NULL AND e.capability <> ''",
            nativeQuery = true)
    long capabilityCallCounts();

    @Query(value = "SELECT COUNT(DISTINCT e.user_hash) FROM telemetry_event e " +
            "WHERE e.capability IS NOT NULL AND e.capability <> '' " +
            "AND e.user_hash IS NOT NULL AND e.user_hash <> ''",
            nativeQuery = true)
    long countDistinctUsersWithCapability();

    @Query(value = "SELECT e.capability AS cap, COUNT(*) AS cnt " +
            "FROM telemetry_event e " +
            "WHERE e.capability IS NOT NULL AND e.capability <> '' " +
            "GROUP BY e.capability ORDER BY cnt DESC",
            nativeQuery = true)
    List<Object[]> capabilityCallCountsByCap();

    @Query(value = "SELECT DATE(e.server_time) AS d, e.capability AS cap, COUNT(*) AS cnt " +
            "FROM telemetry_event e " +
            "WHERE e.capability IS NOT NULL AND e.capability <> '' " +
            "AND DATE(e.server_time) >= :startDate " +
            "GROUP BY DATE(e.server_time), e.capability ORDER BY d, cap",
            nativeQuery = true)
    List<Object[]> capabilityCallsByDate(@Param("startDate") LocalDate startDate);

    @Query(value = "SELECT e.capability AS cap, COUNT(*) AS cnt " +
            "FROM telemetry_event e " +
            "WHERE e.capability IS NOT NULL AND e.capability <> '' " +
            "AND DATE(e.server_time) = :date " +
            "GROUP BY e.capability",
            nativeQuery = true)
    List<Object[]> capabilityCallCountsBySpecificDate(@Param("date") LocalDate date);

    @Query(value = "SELECT COUNT(DISTINCT e.user_hash) FROM telemetry_event e " +
            "WHERE e.capability = :capability " +
            "AND e.user_hash IS NOT NULL AND e.user_hash <> '' " +
            "AND DATE(e.server_time) = :date",
            nativeQuery = true)
    long countDistinctUsersByCapabilityAndDate(@Param("capability") String capability,
                                                @Param("date") LocalDate date);

    @Query(value = "SELECT COUNT(*) FROM telemetry_event e " +
            "WHERE e.capability IS NOT NULL AND e.capability <> '' " +
            "AND DATE(e.server_time) = :date",
            nativeQuery = true)
    long capabilityCallCountByDate(@Param("date") LocalDate date);

    @Query(value = "SELECT e.event_value AS skill, COUNT(*) AS cnt " +
            "FROM telemetry_event e " +
            "WHERE e.capability = 'skill' " +
            "AND e.event_value IS NOT NULL AND e.event_value <> '' " +
            "GROUP BY e.event_value ORDER BY cnt DESC LIMIT :limit",
            nativeQuery = true)
    List<Object[]> skillRanking(@Param("limit") int limit);

    @Query(value = "SELECT DATE(e.server_time) AS d, e.event_value AS skill, COUNT(*) AS cnt " +
            "FROM telemetry_event e " +
            "WHERE e.capability = 'skill' " +
            "AND e.event_value IS NOT NULL AND e.event_value <> '' " +
            "AND DATE(e.server_time) >= :startDate " +
            "GROUP BY DATE(e.server_time), e.event_value ORDER BY d, cnt DESC",
            nativeQuery = true)
    List<Object[]> skillCallsByDate(@Param("startDate") LocalDate startDate);
}
