package com.huaweicloud.hdkitservice.service;

import com.huaweicloud.hdkitservice.config.HdkitTelemetryConfig;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class TelemetryHashService {

    private final HdkitTelemetryConfig config;

    public TelemetryHashService(HdkitTelemetryConfig config) {
        this.config = config;
    }

    public String generateUserHash(String domainId) {
        if (domainId == null || domainId.isEmpty()) {
            return null;
        }
        return sha256(domainId + config.getSalt());
    }

    public String generateFallbackUserHash(String ak) {
        if (ak == null || ak.isEmpty()) {
            return null;
        }
        return sha256(ak + config.getSalt());
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}