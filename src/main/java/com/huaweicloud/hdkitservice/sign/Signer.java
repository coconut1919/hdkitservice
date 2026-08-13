package com.huaweicloud.hdkitservice.sign;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

public final class Signer {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    private Signer() {}

    public static String sha256Hex(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("sha256 failed", e);
        }
    }

    public static String hmacSha256Hex(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("hmac failed", e);
        }
    }

    public static SignResult sign(String method, String path, String query, String body,
                                  String ak, String sk, String host) {
        String timestamp = FMT.format(ZonedDateTime.now(ZoneOffset.UTC));
        return sign(method, path, query, body, ak, sk, host, timestamp);
    }

    public static SignResult sign(String method, String path, String query, String body,
                                  String ak, String sk, String host, String timestamp) {
        String signedHeaders = "host;x-sdk-date";
        String canonicalHeaders = "host:" + host + "\nx-sdk-date:" + timestamp + "\n";
        String uri = path.endsWith("/") ? path : path + "/";
        String canonicalRequest = method + "\n" + uri + "\n" + query + "\n"
                + canonicalHeaders + "\n" + signedHeaders + "\n" + sha256Hex(body);
        String stringToSign = "SDK-HMAC-SHA256\n" + timestamp + "\n" + sha256Hex(canonicalRequest);
        String signature = hmacSha256Hex(sk, stringToSign);
        String authorization = "SDK-HMAC-SHA256 Access=" + ak + ", SignedHeaders=" + signedHeaders
                + ", Signature=" + signature;
        return new SignResult(authorization, timestamp);
    }

    public record SignResult(String authorization, String timestamp) {}
}
