package com.huaweicloud.hdkitservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "hdkit.telemetry")
@Component
public class HdkitTelemetryConfig {

    private String salt = "changeme";

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }
}