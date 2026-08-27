package com.huaweicloud.hdkitservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "telemetry_event")
public class TelemetryEvent {

    @Id
    @Column(name = "event_id", length = 36, nullable = false)
    private String eventId;

    @Column(name = "event_key", length = 128, nullable = false)
    private String eventKey;

    @Column(name = "event_value", length = 256, nullable = false)
    private String eventValue;

    @Column(name = "install_id", length = 64)
    private String installId;

    @Column(name = "user_hash", length = 64)
    private String userHash;

    @Column(name = "plugin_version", length = 32)
    private String pluginVersion;

    @Column(name = "agent_harness", length = 32)
    private String agentHarness;

    @Column(name = "agent_version", length = 32)
    private String agentVersion;

    @Column(name = "os", length = 16)
    private String os;

    @Column(name = "os_version", length = 64)
    private String osVersion;

    @Column(name = "capability", length = 16)
    private String capability;

    @Column(name = "event_time", nullable = false)
    private Long eventTime;

    @Column(name = "server_time", nullable = false)
    private LocalDateTime serverTime;

    public TelemetryEvent() {
    }

    public TelemetryEvent(String eventId, String eventKey, String eventValue, String installId,
                          String userHash, String pluginVersion, String agentHarness, String agentVersion,
                          String os, String osVersion, String capability, Long eventTime, LocalDateTime serverTime) {
        this.eventId = eventId;
        this.eventKey = eventKey;
        this.eventValue = eventValue;
        this.installId = installId;
        this.userHash = userHash;
        this.pluginVersion = pluginVersion;
        this.agentHarness = agentHarness;
        this.agentVersion = agentVersion;
        this.os = os;
        this.osVersion = osVersion;
        this.capability = capability;
        this.eventTime = eventTime;
        this.serverTime = serverTime;
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getEventKey() { return eventKey; }
    public void setEventKey(String eventKey) { this.eventKey = eventKey; }
    public String getEventValue() { return eventValue; }
    public void setEventValue(String eventValue) { this.eventValue = eventValue; }
    public String getInstallId() { return installId; }
    public void setInstallId(String installId) { this.installId = installId; }
    public String getUserHash() { return userHash; }
    public void setUserHash(String userHash) { this.userHash = userHash; }
    public String getPluginVersion() { return pluginVersion; }
    public void setPluginVersion(String pluginVersion) { this.pluginVersion = pluginVersion; }
    public String getAgentHarness() { return agentHarness; }
    public void setAgentHarness(String agentHarness) { this.agentHarness = agentHarness; }
    public String getAgentVersion() { return agentVersion; }
    public void setAgentVersion(String agentVersion) { this.agentVersion = agentVersion; }
    public String getOs() { return os; }
    public void setOs(String os) { this.os = os; }
    public String getOsVersion() { return osVersion; }
    public void setOsVersion(String osVersion) { this.osVersion = osVersion; }
    public String getCapability() { return capability; }
    public void setCapability(String capability) { this.capability = capability; }
    public Long getEventTime() { return eventTime; }
    public void setEventTime(Long eventTime) { this.eventTime = eventTime; }
    public LocalDateTime getServerTime() { return serverTime; }
    public void setServerTime(LocalDateTime serverTime) { this.serverTime = serverTime; }
}