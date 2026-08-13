package com.huaweicloud.hdkitservice.service;

import com.huaweicloud.hdkitservice.config.HdkitConfig;
import com.huaweicloud.hdkitservice.model.CheckUserResponse;
import com.huaweicloud.hdkitservice.model.ConnectRequest;
import com.huaweicloud.hdkitservice.model.ConnectResponse;
import com.huaweicloud.hdkitservice.model.CredentialsRequest;
import com.huaweicloud.hdkitservice.model.CredentialsResponse;
import com.huaweicloud.hdkitservice.model.ReleaseRequest;
import com.huaweicloud.hdkitservice.model.ReleaseResponse;
import com.huaweicloud.hdkitservice.model.SandboxSession;
import com.huaweicloud.hdkitservice.model.SignAgreementResponse;
import com.huaweicloud.hdkitservice.sign.Signer;
import com.huaweicloud.hdkitservice.store.SessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

@Service
public class SandboxService {

    private static final Logger log = LoggerFactory.getLogger(SandboxService.class);
    private static final String STATUS_READY = "0004";
    private static final String STATUS_RUNNING = "0002";

    private final DevStationClient devStation;
    private final SessionStore store;
    private final HdkitConfig config;

    public SandboxService(DevStationClient devStation, SessionStore store, HdkitConfig config) {
        this.devStation = devStation;
        this.store = store;
        this.config = config;
    }

    public ConnectResponse connect(ConnectRequest req, String ak, String sk) {
        String userKey = Signer.sha256Hex(ak);
        String templateId = (req.templateId() == null || req.templateId().isEmpty())
                ? config.templateId() : req.templateId();
        String flavorId = (req.flavorId() == null || req.flavorId().isEmpty())
                ? config.flavorId() : req.flavorId();

        List<DevStationClient.Devenv> actual = devStation.list("", ak, sk);
        reconcileStaleSessions(userKey, actual);

        DevStationClient.Devenv existing = findHcdkInstance(actual);
        String devStageId;
        String name;
        boolean created = false;

        if (existing != null) {
            // 复用已有实例
            devStageId = existing.id();
            name = existing.name();
        } else {
            if (store.countActive() >= config.maxConcurrent()) {
                throw new HdkitException("HDKIT_CONFLICT", "已达最大并发沙箱数 " + config.maxConcurrent(), null);
            }
            // 新建实例（name 内部生成，保证唯一可识别）
            name = "hcdk" + Long.toString(System.currentTimeMillis(), 36);
            devStageId = devStation.create(name, templateId, flavorId, req.source(), req.env(), req.git(), ak, sk);
            created = true;
            waitForStatus(devStageId, STATUS_READY, config.connectTimeout(), ak, sk);
        }

        try {
            ensureRunning(devStageId, ak, sk);
            devStation.autoConfig(devStageId, true, ak, sk); // 注入临时 AK/SK

            DevStationClient.Connections conns = devStation.connections(devStageId, config.source(), ak, sk);
            long connectionId = pickConnected(conns);
            DevStationClient.ConnectionAddress addr = devStation.address(devStageId, connectionId, ak, sk);
            String address = addr.url() + "&source=" + addr.source();

            String sessionId = upsertSession(userKey, devStageId, name, String.valueOf(connectionId), address);
            return new ConnectResponse(sessionId, devStageId, String.valueOf(connectionId), address, "connected");
        } catch (Exception e) {
            log.error("[connect] failed: {}", e.getMessage());
            if (created) {
                try { releaseById(devStageId, ak, sk); } catch (Exception ex) {
                    log.error("[connect] rollback release failed: {}", ex.getMessage());
                }
            }
            throw new HdkitException("HDKIT_CONNECT_FAILED", "连接沙箱失败", e);
        }
    }

    private DevStationClient.Devenv findHcdkInstance(List<DevStationClient.Devenv> actual) {
        for (DevStationClient.Devenv d : actual) {
            if (d.name() != null && d.name().startsWith("hcdk")) {
                return d;
            }
        }
        return null;
    }

    private void reconcileStaleSessions(String userKey, List<DevStationClient.Devenv> actual) {
        Set<String> actualIds = actual.stream()
                .map(DevStationClient.Devenv::id)
                .collect(Collectors.toSet());
        for (SandboxSession s : store.listAll()) {
            if (userKey.equals(s.userKey()) && !actualIds.contains(s.devStageId())) {
                log.info("[connect] prune stale session {} (dev env {} gone)", s.sessionId(), s.devStageId());
                store.delete(s.sessionId());
            }
        }
    }

    private void ensureRunning(String devStageId, String ak, String sk) {
        String status = devStation.statusOf(devStageId, ak, sk);
        if (!isStatus(status, STATUS_RUNNING)) {
            devStation.start(devStageId, config.source(), ak, sk);
            waitForStatus(devStageId, STATUS_RUNNING, config.connectTimeout(), ak, sk);
        }
    }

    private String upsertSession(String userKey, String devStageId, String name, String connectionId, String address) {
        String existing = store.findByDevStageId(devStageId);
        String sessionId = (existing != null && !existing.isEmpty())
                ? existing
                : UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        long now = System.currentTimeMillis();
        store.save(new SandboxSession(sessionId, userKey, name, devStageId, connectionId, address, "connected", now, now));
        store.addActive(sessionId);
        return sessionId;
    }

    public CredentialsResponse credentials(CredentialsRequest req, String ak, String sk) {
        String userKey = Signer.sha256Hex(ak);
        String devStageId = resolveDevStageId(req.sessionId(), req.devStageId());
        if (devStageId == null) {
            throw new HdkitException("HDKIT_INVALID_REQUEST", "缺少 session_id 或 dev_stage_id", null);
        }

        String status = devStation.statusOf(devStageId, ak, sk);
        if (!isStatus(status, STATUS_RUNNING)) {
            throw new HdkitException("HDKIT_NOT_RUNNING", "环境未处于 RUNNING，无法注入临时 AK/SK", null);
        }

        boolean enableSts = req.enableSts() == null || req.enableSts();
        String expiresAt = devStation.autoConfig(devStageId, enableSts, ak, sk);

        String existing = store.findByDevStageId(devStageId);
        String sessionId;
        long now = System.currentTimeMillis();
        if (existing != null) {
            SandboxSession old = store.get(existing);
            sessionId = existing;
            store.save(new SandboxSession(sessionId, userKey, old != null ? old.name() : "", devStageId,
                    old != null ? old.connectionId() : "", old != null ? old.address() : "",
                    old != null ? old.status() : "connected", old != null ? old.createdAt() : now, now));
        } else {
            sessionId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            store.save(new SandboxSession(sessionId, userKey, "", devStageId, "", "", "connected", now, now));
        }
        return new CredentialsResponse(sessionId, expiresAt);
    }

    public ReleaseResponse release(ReleaseRequest req, String ak, String sk) {
        String devStageId = resolveDevStageId(req.sessionId(), req.devStageId());
        if (devStageId == null) {
            throw new HdkitException("HDKIT_INVALID_REQUEST", "缺少 session_id 或 dev_stage_id", null);
        }
        try {
            releaseById(devStageId, ak, sk);
            String sid = store.findByDevStageId(devStageId);
            if (sid != null) store.delete(sid);
            return new ReleaseResponse(true, devStageId);
        } catch (Exception e) {
            log.error("[release] failed for {}: {}", devStageId, e.getMessage());
            String sid = store.findByDevStageId(devStageId);
            if (sid != null) {
                SandboxSession old = store.get(sid);
                if (old != null) {
                    store.save(new SandboxSession(sid, old.userKey(), old.name(), devStageId, old.connectionId(),
                            old.address(), "release_failed", old.createdAt(), System.currentTimeMillis()));
                }
            }
            throw new HdkitException("HDKIT_RELEASE_FAILED", "释放沙箱失败", e);
        }
    }

    public void releaseById(String devStageId, String ak, String sk) {
        if (devStation.statusOf(devStageId, ak, sk) == null) {
            return; // 幂等：环境已不存在，视为已释放
        }
        devStation.close(devStageId, config.source(), ak, sk);
        waitForStatus(devStageId, STATUS_READY, config.releaseTimeout(), ak, sk);
        devStation.delete(devStageId, config.source(), ak, sk);
        waitForGone(devStageId, config.releaseTimeout(), ak, sk);
    }

    public CheckUserResponse checkUser(String ak, String sk) {
        String userKey = Signer.sha256Hex(ak);
        boolean realnameCached = store.isRealnameVerified(userKey);
        boolean agreementCached = store.isAgreementSigned(userKey);

        CompletableFuture<Boolean> realnameFuture = CompletableFuture.supplyAsync(
                () -> realnameCached || "2".equals(devStation.realNameStatus(ak, sk)));
        CompletableFuture<Boolean> agreementFuture = CompletableFuture.supplyAsync(
                () -> agreementCached || allAgreementsSigned(devStation.agreements(ak, sk)));

        boolean realnameOk = await(realnameFuture, "查询实名状态失败");
        boolean agreementOk = await(agreementFuture, "查询协议状态失败");

        if (realnameOk && !realnameCached) store.cacheRealnameVerified(userKey);
        if (agreementOk && !agreementCached) store.cacheAgreementSigned(userKey);

        if (!realnameOk) throw new HdkitException("HDKIT_NOT_REALNAME", "用户未完成实名认证", null);
        if (!agreementOk) throw new HdkitException("HDKIT_NOT_AGREEMENT", "用户未签署协议", null);
        return new CheckUserResponse(true, true);
    }

    public SignAgreementResponse signAgreement(String ak, String sk) {
        String userKey = Signer.sha256Hex(ak);
        List<DevStationClient.Agreement> agreements = devStation.agreements(ak, sk);
        List<DevStationClient.SignReq> toSign = new ArrayList<>();
        for (DevStationClient.Agreement a : agreements) {
            if (a.signStatus() == 2 || a.signStatus() == 3) {
                toSign.add(new DevStationClient.SignReq(a.agrType(), a.country(), a.language(), a.version()));
            }
        }
        if (!toSign.isEmpty()) {
            devStation.signAgreements(toSign, ak, sk);
        }
        store.cacheAgreementSigned(userKey);
        return new SignAgreementResponse(true, toSign.size());
    }

    private boolean allAgreementsSigned(List<DevStationClient.Agreement> agreements) {
        if (agreements.isEmpty()) return false;
        for (DevStationClient.Agreement a : agreements) {
            if (a.signStatus() != 1 && a.signStatus() != 2) return false;
        }
        return true;
    }

    private boolean await(CompletableFuture<Boolean> f, String errMsg) {
        try {
            return f.join();
        } catch (CompletionException e) {
            Throwable c = e.getCause();
            if (c instanceof DevStationClient.DevStationException) {
                throw new HdkitException("HDKIT_UPSTREAM_ERROR", errMsg, c);
            }
            throw new HdkitException("HDKIT_INTERNAL", errMsg, c);
        }
    }

    private long pickConnected(DevStationClient.Connections conns) {
        for (DevStationClient.Conn c : conns.list()) {
            if ("CONNECTED".equals(c.status())) return c.connectionId();
        }
        return conns.connectionId();
    }

    private void waitForStatus(String devStageId, String target, long timeoutMs, String ak, String sk) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            String status = devStation.statusOf(devStageId, ak, sk);
            if (isStatus(status, target)) return;
            sleep(config.pollIntervalMs());
        }
        throw new HdkitException("HDKIT_TIMEOUT", "等待状态 " + target + " 超时", null);
    }

    private boolean isStatus(String actual, String code) {
        if (actual == null) return false;
        int dot = actual.lastIndexOf('.');
        return (dot >= 0 ? actual.substring(dot + 1) : actual).equals(code);
    }

    private void waitForGone(String devStageId, long timeoutMs, String ak, String sk) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (devStation.statusOf(devStageId, ak, sk) == null) return;
            sleep(config.pollIntervalMs());
        }
        throw new HdkitException("HDKIT_RELEASE_TIMEOUT", "等待释放完成超时", null);
    }

    private String resolveDevStageId(String sessionId, String devStageId) {
        if (sessionId != null && !sessionId.isEmpty()) {
            SandboxSession s = store.get(sessionId);
            return s != null ? s.devStageId() : null;
        }
        return (devStageId != null && !devStageId.isEmpty()) ? devStageId : null;
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static class HdkitException extends RuntimeException {
        private final String code;
        public HdkitException(String code, String message, Throwable cause) {
            super(message, cause);
            this.code = code;
        }
        public String code() { return code; }
    }
}
