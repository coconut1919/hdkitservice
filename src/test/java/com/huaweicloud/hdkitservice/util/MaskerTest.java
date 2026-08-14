package com.huaweicloud.hdkitservice.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huaweicloud.hdkitservice.config.HdkitConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaskerTest {

    private Masker masker;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        HdkitConfig config = new HdkitConfig();
        config.setMaskKeys("password,passwd,pwd,secret,token,sk,access_key,private_key,authorization,cookie,credential");
        masker = new Masker(config);
    }

    private JsonNode read(String json) throws Exception {
        return mapper.readTree(json);
    }

    @Test
    void masksSensitiveJsonFieldPartially() throws Exception {
        String out = masker.mask("{\"password\":\"abcdefghij\",\"name\":\"n1\"}");
        JsonNode root = read(out);
        String masked = root.get("password").asText();
        assertTrue(masked.contains("*"));
        assertFalse(masked.contains("cdef"));
        assertEquals('a', masked.charAt(0));
        assertEquals('j', masked.charAt(masked.length() - 1));
        assertEquals("n1", root.get("name").asText());
    }

    @Test
    void masksKeyVariantsAndCaseInsensitive() throws Exception {
        String out = masker.mask("{\"db_password\":\"x\",\"AccessToken\":\"x\",\"SECRET\":\"x\"}");
        JsonNode root = read(out);
        assertNotEquals("x", root.get("db_password").asText());
        assertNotEquals("x", root.get("AccessToken").asText());
        assertNotEquals("x", root.get("SECRET").asText());
    }

    @Test
    void shortKeywordSkMatchesBoundariesOnly() throws Exception {
        String out = masker.mask("{\"task\":\"t1\",\"ak_sk\":\"abc\",\"sk\":\"xyz\"}");
        JsonNode root = read(out);
        assertEquals("t1", root.get("task").asText());
        assertNotEquals("abc", root.get("ak_sk").asText());
        assertNotEquals("xyz", root.get("sk").asText());
    }

    @Test
    void masksNestedObjectAndArray() throws Exception {
        String out = masker.mask("{\"git\":{\"token\":\"t123456\"},\"list\":[{\"password\":\"p123456\"},{\"x\":\"keep\"}]}");
        JsonNode root = read(out);
        assertNotEquals("t123456", root.get("git").get("token").asText());
        assertNotEquals("p123456", root.get("list").get(0).get("password").asText());
        assertEquals("keep", root.get("list").get(1).get("x").asText());
    }

    @Test
    void masksSensitiveUrlQueryParamOnly() throws Exception {
        String out = masker.mask("{\"url\":\"wss://example/1?token=secretvalue&x=1\"}");
        JsonNode root = read(out);
        String url = root.get("url").asText();
        assertTrue(url.contains("token="));
        assertFalse(url.contains("secretvalue"));
        assertTrue(url.contains("x=1"));
    }

    @Test
    void leavesNonSensitiveUrlUntouched() {
        String out = masker.mask("{\"url\":\"wss://example/1?session=abc&x=1\"}");
        assertTrue(out.contains("wss://example/1?session=abc&x=1"));
    }

    @Test
    void truncatesLongContent() {
        String longText = "{\"name\":\"" + "a".repeat(3000) + "\"}";
        String out = masker.mask(longText);
        assertTrue(out.length() <= 2000 + "...(truncated)".length());
        assertTrue(out.endsWith("...(truncated)"));
    }

    @Test
    void nonJsonPassthrough() {
        assertEquals("hello", masker.mask("hello"));
        assertEquals("", masker.mask(null));
        assertEquals("", masker.mask(""));
    }

    @Test
    void customKeysFromConfig() throws Exception {
        HdkitConfig custom = new HdkitConfig();
        custom.setMaskKeys("apikey");
        Masker m = new Masker(custom);
        String out = m.mask("{\"apikey\":\"abc123456789\",\"password\":\"plain\"}");
        JsonNode root = read(out);
        assertNotEquals("abc123456789", root.get("apikey").asText());
        assertEquals("plain", root.get("password").asText());
    }

    @Test
    void maskQueryMasksSensitiveParamOnly() {
        assertEquals("session_id=x&token=12*****89",
                masker.maskQuery("session_id=x&token=123456789"));
        assertEquals("plain", masker.maskQuery("plain"));
        assertEquals("", masker.maskQuery(null));
    }

    @Test
    void partialKeepsAboutFortyPercentVisible() {
        String out = masker.mask("{\"token\":\"0123456789\"}");
        JsonNode root;
        try {
            root = read(out);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String masked = root.get("token").asText();
        assertTrue(masked.startsWith("01"));
        assertTrue(masked.endsWith("89"));
        assertTrue(masked.contains("*"));
    }
}
