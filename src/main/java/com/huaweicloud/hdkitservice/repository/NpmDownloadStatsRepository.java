package com.huaweicloud.hdkitservice.repository;

import com.huaweicloud.hdkitservice.model.NpmDownloadStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface NpmDownloadStatsRepository extends JpaRepository<NpmDownloadStats, LocalDate> {

    Optional<NpmDownloadStats> findByStatDate(LocalDate statDate);

    @Query("SELECT n FROM NpmDownloadStats n WHERE n.statDate >= :startDate ORDER BY n.statDate")
    List<NpmDownloadStats> findSince(@Param("startDate") LocalDate startDate);

    @Query("SELECT n FROM NpmDownloadStats n ORDER BY n.statDate DESC LIMIT 1")
    Optional<NpmDownloadStats> findLatest();
}
