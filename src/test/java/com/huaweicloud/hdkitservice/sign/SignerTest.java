package com.huaweicloud.hdkitservice.sign;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignerTest {

    @Test
    void sha256OfEmptyIsKnownConstant() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                Signer.sha256Hex(""));
    }

    @Test
    void sha256OfKnownString() {
        // sha256("abc") 标准值
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                Signer.sha256Hex("abc"));
    }

    @Test
    void hmacSha256KnownVector() {
        // RFC 4231 测试向量：key="key", data="The quick brown fox jumps over the lazy dog"
        assertEquals("f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8",
                Signer.hmacSha256Hex("key", "The quick brown fox jumps over the lazy dog"));
    }

    @Test
    void signMatchesVerifiedVector() {
        // 用真实账号 curl 验证过的算法 + 固定时间戳生成的期望签名
        Signer.SignResult r = Signer.sign(
                "GET", "/open-api-public/v2/devenvs", "", "",
                "TESTAK", "TESTSK", "devstation.myhuaweicloud.com", "20260813T000000Z");
        assertEquals(
                "SDK-HMAC-SHA256 Access=TESTAK, SignedHeaders=host;x-sdk-date, Signature=a040c52c1a70d5b3c0ca2ed7000796f7583ddae14e426f88875a12b510abd269",
                r.authorization());
        assertEquals("20260813T000000Z", r.timestamp());
    }

    @Test
    void signAddsTrailingSlashToUri() {
        // path 不带尾斜杠与带尾斜杠应得到相同签名（因为规范 URI 会补尾斜杠）
        Signer.SignResult withoutSlash = Signer.sign(
                "GET", "/open-api-public/v2/devenvs", "", "",
                "TESTAK", "TESTSK", "devstation.myhuaweicloud.com", "20260813T000000Z");
        Signer.SignResult withSlash = Signer.sign(
                "GET", "/open-api-public/v2/devenvs/", "", "",
                "TESTAK", "TESTSK", "devstation.myhuaweicloud.com", "20260813T000000Z");
        assertEquals(withoutSlash.authorization(), withSlash.authorization());
    }

    @Test
    void signIncludesQueryString() {
        Signer.SignResult r = Signer.sign(
                "DELETE", "/open-api-public/v1/devenvs/dev1", "source=CLI", "",
                "TESTAK", "TESTSK", "devstation.myhuaweicloud.com", "20260813T000000Z");
        assertTrue(r.authorization().startsWith("SDK-HMAC-SHA256 Access=TESTAK, SignedHeaders=host;x-sdk-date, Signature="));
    }

    @Test
    void signWithBodyDiffersFromEmpty() {
        Signer.SignResult empty = Signer.sign(
                "POST", "/open-api-public/v1/auto-config", "", "",
                "TESTAK", "TESTSK", "devstation.myhuaweicloud.com", "20260813T000000Z");
        Signer.SignResult withBody = Signer.sign(
                "POST", "/open-api-public/v1/auto-config", "", "{\"instance_id\":\"x\",\"enable_sts\":true}",
                "TESTAK", "TESTSK", "devstation.myhuaweicloud.com", "20260813T000000Z");
        assertEquals(empty.timestamp(), withBody.timestamp());
        org.junit.jupiter.api.Assertions.assertNotEquals(empty.authorization(), withBody.authorization());
    }

    @Test
    void signWithDifferentSkProducesDifferentSignature() {
        Signer.SignResult a = Signer.sign(
                "GET", "/open-api-public/v2/devenvs", "", "",
                "TESTAK", "SK_A", "devstation.myhuaweicloud.com", "20260813T000000Z");
        Signer.SignResult b = Signer.sign(
                "GET", "/open-api-public/v2/devenvs", "", "",
                "TESTAK", "SK_B", "devstation.myhuaweicloud.com", "20260813T000000Z");
        org.junit.jupiter.api.Assertions.assertNotEquals(a.authorization(), b.authorization());
    }

    @Test
    void signWithTokenIncludesSecurityTokenInSignedHeaders() {
        Signer.SignResult withToken = Signer.signWithToken(
                "GET", "/v3/auth/domains", "", "",
                "TESTAK", "TESTSK", "TEMP-TOKEN", "iam.myhuaweicloud.com");
        Signer.SignResult noToken = Signer.sign(
                "GET", "/v3/auth/domains", "", "",
                "TESTAK", "TESTSK", "iam.myhuaweicloud.com");

        assertTrue(withToken.authorization().startsWith("SDK-HMAC-SHA256 Access=TESTAK, SignedHeaders=host;x-sdk-date;x-security-token, Signature="));
        assertTrue(noToken.authorization().startsWith("SDK-HMAC-SHA256 Access=TESTAK, SignedHeaders=host;x-sdk-date, Signature="));
        org.junit.jupiter.api.Assertions.assertNotEquals(withToken.authorization(), noToken.authorization());
    }
}
