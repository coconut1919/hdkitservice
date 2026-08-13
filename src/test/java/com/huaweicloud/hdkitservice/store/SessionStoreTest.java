package com.huaweicloud.hdkitservice.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huaweicloud.hdkitservice.config.HdkitConfig;
import com.huaweicloud.hdkitservice.model.SandboxSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionStoreTest {

    private StringRedisTemplate redis;
    private ValueOperations<String, String> valueOps;
    private SetOperations<String, String> setOps;
    private SessionStore store;
    private final ObjectMapper mapper = new ObjectMapper();

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        setOps = mock(SetOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(redis.opsForSet()).thenReturn(setOps);

        HdkitConfig config = new HdkitConfig();
        config.setEndpoint("https://devstation.myhuaweicloud.com");
        store = new SessionStore(redis, mapper, config);
    }

    @Test
    void saveWritesSessionAndReverseIndex() {
        SandboxSession s = new SandboxSession("s1", "uk1", "hcdk1", "dev1", "1", "wss://x", "connected", 1L, 2L);
        store.save(s);

        verify(valueOps).set(eq("hdkitservice:sandbox:s1"), anyString(), any(Duration.class));
        verify(valueOps).set(eq("hdkitservice:sandbox:by-devstage:dev1"), eq("s1"), any(Duration.class));
    }

    @Test
    void getReturnsParsedSession() throws Exception {
        SandboxSession s = new SandboxSession("s1", "uk1", "hcdk1", "dev1", "1", "wss://x", "connected", 1L, 2L);
        when(valueOps.get("hdkitservice:sandbox:s1")).thenReturn(mapper.writeValueAsString(s));

        SandboxSession got = store.get("s1");
        assertNotNull(got);
        assertEquals("dev1", got.devStageId());
        assertEquals("connected", got.status());
    }

    @Test
    void getReturnsNullWhenMissing() {
        when(valueOps.get("hdkitservice:sandbox:s1")).thenReturn(null);
        assertNull(store.get("s1"));
    }

    @Test
    void getReturnsNullWhenJsonBroken() {
        when(valueOps.get("hdkitservice:sandbox:s1")).thenReturn("not-json");
        assertNull(store.get("s1"));
    }

    @Test
    void deleteRemovesKeyReverseIndexAndActive() {
        String json = "{\"sessionId\":\"s1\",\"name\":\"hcdk1\",\"devStageId\":\"dev1\",\"connectionId\":\"1\","
                + "\"address\":\"wss://x\",\"status\":\"release_failed\",\"createdAt\":1,\"updatedAt\":2}";
        when(valueOps.get("hdkitservice:sandbox:s1")).thenReturn(json);

        store.delete("s1");
        verify(redis).delete("hdkitservice:sandbox:s1");
        verify(redis).delete("hdkitservice:sandbox:by-devstage:dev1");
        verify(setOps).remove("hdkitservice:sandbox:active", "s1");
    }

    @Test
    void findByDevStageId() {
        when(valueOps.get("hdkitservice:sandbox:by-devstage:dev1")).thenReturn("s1");
        assertEquals("s1", store.findByDevStageId("dev1"));
    }

    @Test
    void countActive() {
        when(setOps.size("hdkitservice:sandbox:active")).thenReturn(3L);
        assertEquals(3L, store.countActive());
    }

    @Test
    void countActiveReturnsZeroWhenNull() {
        when(setOps.size("hdkitservice:sandbox:active")).thenReturn(null);
        assertEquals(0L, store.countActive());
    }

    @Test
    void addAndRemoveActive() {
        store.addActive("s1");
        verify(setOps).add("hdkitservice:sandbox:active", "s1");
        store.removeActive("s1");
        verify(setOps).remove("hdkitservice:sandbox:active", "s1");
    }

    @Test
    void pruneActiveRemovesExpiredEntries() {
        when(setOps.members("hdkitservice:sandbox:active")).thenReturn(Set.of("s1", "s2"));
        when(redis.hasKey("hdkitservice:sandbox:s1")).thenReturn(true);
        when(redis.hasKey("hdkitservice:sandbox:s2")).thenReturn(false);

        store.pruneActive();

        verify(setOps).remove("hdkitservice:sandbox:active", "s2");
        org.mockito.Mockito.verify(setOps, org.mockito.Mockito.never())
                .remove("hdkitservice:sandbox:active", "s1");
    }

    @Test
    void listReleaseFailedFiltersByStatus() {
        String failed = "{\"sessionId\":\"s1\",\"name\":\"n1\",\"devStageId\":\"dev1\",\"connectionId\":\"1\","
                + "\"address\":\"wss://x\",\"status\":\"release_failed\",\"createdAt\":1,\"updatedAt\":2}";
        String connected = "{\"sessionId\":\"s2\",\"name\":\"n2\",\"devStageId\":\"dev2\",\"connectionId\":\"2\","
                + "\"address\":\"wss://y\",\"status\":\"connected\",\"createdAt\":1,\"updatedAt\":2}";
        when(redis.keys("hdkitservice:sandbox:*"))
                .thenReturn(Set.of("hdkitservice:sandbox:s1", "hdkitservice:sandbox:s2"));
        when(valueOps.get("hdkitservice:sandbox:s1")).thenReturn(failed);
        when(valueOps.get("hdkitservice:sandbox:s2")).thenReturn(connected);

        List<SandboxSession> result = store.listReleaseFailed();
        assertEquals(1, result.size());
        assertEquals("dev1", result.get(0).devStageId());
    }

    @Test
    void listAllSkipsActiveSetAndReverseIndexKeys() {
        when(redis.keys("hdkitservice:sandbox:*")).thenReturn(Set.of(
                "hdkitservice:sandbox:active",
                "hdkitservice:sandbox:by-devstage:dev1",
                "hdkitservice:sandbox:s1"));
        when(valueOps.get("hdkitservice:sandbox:s1")).thenReturn(
                "{\"sessionId\":\"s1\",\"userKey\":\"uk\",\"name\":\"n1\",\"devStageId\":\"dev1\","
                        + "\"connectionId\":\"1\",\"address\":\"wss://x\",\"status\":\"connected\","
                        + "\"createdAt\":1,\"updatedAt\":2}");

        List<SandboxSession> result = store.listAll();

        assertEquals(1, result.size());
        assertEquals("s1", result.get(0).sessionId());
        verify(valueOps, never()).get("hdkitservice:sandbox:active");
        verify(valueOps, never()).get("hdkitservice:sandbox:by-devstage:dev1");
    }
}
