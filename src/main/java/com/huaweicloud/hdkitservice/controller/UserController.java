package com.huaweicloud.hdkitservice.controller;

import com.huaweicloud.hdkitservice.service.IncentiveClient;
import com.huaweicloud.hdkitservice.service.TelemetryHashService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/rest/developer/server/hdkitservice")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final IncentiveClient incentiveClient;
    private final TelemetryHashService hashService;

    public UserController(IncentiveClient incentiveClient, TelemetryHashService hashService) {
        this.incentiveClient = incentiveClient;
        this.hashService = hashService;
    }

    @GetMapping("/user/generatorUserIDHash")
    public ResponseEntity<Map<String, String>> generatorUserIDHash(@RequestHeader("X-HW-AK") String ak,
                                                                    @RequestHeader("X-HW-SK") String sk) {
        String domainId;
        try {
            domainId = incentiveClient.resolveDomainIdFromIam(ak, sk);
        } catch (IncentiveClient.IncentiveException e) {
            log.warn("[generatorUserIDHash] IAM failed, using AK fallback: {}", e.getMessage());
            String hash = hashService.generateFallbackUserHash(ak);
            return ResponseEntity.ok(Map.of("userHash", hash));
        }

        if (domainId != null && !domainId.isEmpty()) {
            return ResponseEntity.ok(Map.of("userHash", hashService.generateUserHash(domainId)));
        }

        return ResponseEntity.ok(Map.of("userHash", hashService.generateFallbackUserHash(ak)));
    }
}