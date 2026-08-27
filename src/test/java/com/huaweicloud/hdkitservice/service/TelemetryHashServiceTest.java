package com.huaweicloud.hdkitservice.service;

import com.huaweicloud.hdkitservice.config.HdkitTelemetryConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class TelemetryHashServiceTest {

    private TelemetryHashService hashService;

    @BeforeEach
    void setUp() {
        HdkitTelemetryConfig config = new HdkitTelemetryConfig();
        config.setSalt("test-salt");
        hashService = new TelemetryHashService(config);
    }

    @Test
    void generateUserHashReturnsSha256() {
        String hash = hashService.generateUserHash("domain123");
        assertNotNull(hash);
        assertEquals(64, hash.length());
    }

    @Test
    void generateUserHashDeterministic() {
        String h1 = hashService.generateUserHash("domain123");
        String h2 = hashService.generateUserHash("domain123");
        assertEquals(h1, h2);
    }

    @Test
    void generateUserHashDifferentDomainIdsProduceDifferentHashes() {
        String h1 = hashService.generateUserHash("domainA");
        String h2 = hashService.generateUserHash("domainB");
        assertNotNull(h1);
        assertNotNull(h2);
        assertEquals(false, h1.equals(h2));
    }

    @Test
    void generateUserHashNullDomainIdReturnsNull() {
        assertNull(hashService.generateUserHash(null));
    }

    @Test
    void generateUserHashEmptyDomainIdReturnsNull() {
        assertNull(hashService.generateUserHash(""));
    }

    @Test
    void generateFallbackUserHashReturnsSha256() {
        String hash = hashService.generateFallbackUserHash("AK123");
        assertNotNull(hash);
        assertEquals(64, hash.length());
    }

    @Test
    void generateFallbackUserHashNullAkReturnsNull() {
        assertNull(hashService.generateFallbackUserHash(null));
    }

    @Test
    void generateFallbackUserHashEmptyAkReturnsNull() {
        assertNull(hashService.generateFallbackUserHash(""));
    }

    @Test
    void differentSaltProducesDifferentHash() {
        HdkitTelemetryConfig config2 = new HdkitTelemetryConfig();
        config2.setSalt("different-salt");
        TelemetryHashService hashService2 = new TelemetryHashService(config2);

        String h1 = hashService.generateUserHash("domain123");
        String h2 = hashService2.generateUserHash("domain123");
        assertEquals(false, h1.equals(h2));
    }
}