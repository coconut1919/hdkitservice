package com.huaweicloud.hdkitservice.repository;

import com.huaweicloud.hdkitservice.model.TelemetryEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TelemetryEventRepository extends JpaRepository<TelemetryEvent, String> {
}