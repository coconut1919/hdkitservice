package com.huaweicloud.hdkitservice.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huaweicloud.hdkitservice.config.HdkitConfig;
import com.huaweicloud.hdkitservice.model.SandboxSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class SessionStore {

    private static final Logger log = LoggerFactory.getLogger(SessionStore.class);
    private static final String KEY_PREFIX = "hdkitservice:sandbox:";
    private static final String BY_DEVSTAGE_PREFIX = "hdkitservice:sandbox:by-devstage:";
    private static final String ACTIVE_SET = "hdkitservice:sandbox:active";
    private static final String USER_KEY_PREFIX = "hdkitservice:user:";

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final HdkitConfig config;

    public SessionStore(StringRedisTemplate redis, ObjectMapper mapper, HdkitConfig config) {
        this.redis = redis;
        this.mapper = mapper;
        this.config = config;
    }

    public void save(SandboxSession s) {
        try {
            String json = mapper.writeValueAsString(s);
            redis.opsForValue().set(KEY_PREFIX + s.sessionId(), json, Duration.ofSeconds(config.sessionTtl()));
            redis.opsForValue().set(BY_DEVSTAGE_PREFIX + s.devStageId(), s.sessionId(),
                    Duration.ofSeconds(config.sessionTtl()));
        } catch (Exception e) {
            log.error("[session] save failed: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public SandboxSession get(String sessionId) {
        String json = redis.opsForValue().get(KEY_PREFIX + sessionId);
        if (json == null) return null;
        try {
            return mapper.readValue(json, SandboxSession.class);
        } catch (Exception e) {
            log.error("[session] parse failed: {}", e.getMessage());
            return null;
        }
    }

    public void delete(String sessionId) {
        SandboxSession s = get(sessionId);
        redis.delete(KEY_PREFIX + sessionId);
        redis.opsForSet().remove(ACTIVE_SET, sessionId);
        if (s != null) redis.delete(BY_DEVSTAGE_PREFIX + s.devStageId());
    }

    public String findByDevStageId(String devStageId) {
        return redis.opsForValue().get(BY_DEVSTAGE_PREFIX + devStageId);
    }

    public long countActive() {
        Long n = redis.opsForSet().size(ACTIVE_SET);
        return n == null ? 0 : n;
    }

    public void addActive(String sessionId) {
        redis.opsForSet().add(ACTIVE_SET, sessionId);
    }

    public void removeActive(String sessionId) {
        redis.opsForSet().remove(ACTIVE_SET, sessionId);
    }

    public List<SandboxSession> listReleaseFailed() {
        List<SandboxSession> out = new ArrayList<>();
        Set<String> keys = redis.keys(KEY_PREFIX + "*");
        if (keys == null) return out;
        for (String key : keys) {
            SandboxSession s = get(key.substring(KEY_PREFIX.length()));
            if (s != null && "release_failed".equals(s.status())) out.add(s);
        }
        return out;
    }

    public List<SandboxSession> listAll() {
        List<SandboxSession> out = new ArrayList<>();
        Set<String> keys = redis.keys(KEY_PREFIX + "*");
        if (keys == null) return out;
        for (String key : keys) {
            SandboxSession s = get(key.substring(KEY_PREFIX.length()));
            if (s != null) out.add(s);
        }
        return out;
    }

    public void pruneActive() {
        Set<String> active = redis.opsForSet().members(ACTIVE_SET);
        if (active == null) return;
        for (String sid : active) {
            if (Boolean.FALSE.equals(redis.hasKey(KEY_PREFIX + sid))) {
                redis.opsForSet().remove(ACTIVE_SET, sid);
            }
        }
    }

    public boolean isRealnameVerified(String userKey) {
        return "ok".equals(redis.opsForValue().get(USER_KEY_PREFIX + userKey + ":realname"));
    }

    public void cacheRealnameVerified(String userKey) {
        redis.opsForValue().set(USER_KEY_PREFIX + userKey + ":realname", "ok",
                Duration.ofSeconds(config.sessionTtl()));
    }

    public boolean isAgreementSigned(String userKey) {
        return "ok".equals(redis.opsForValue().get(USER_KEY_PREFIX + userKey + ":agreement"));
    }

    public void cacheAgreementSigned(String userKey) {
        redis.opsForValue().set(USER_KEY_PREFIX + userKey + ":agreement", "ok",
                Duration.ofSeconds(config.sessionTtl()));
    }
}
