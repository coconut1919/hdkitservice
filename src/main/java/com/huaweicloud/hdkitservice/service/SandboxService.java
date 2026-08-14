package com.huaweicloud.hdkitservice.service;

import com.huaweicloud.hdkitservice.config.HdkitConfig;
import com.huaweicloud.hdkitservice.model.CheckUserResponse;
import com.huaweicloud.hdkitservice.model.ConnectRequest;
import com.huaweicloud.hdkitservice.model.ConnectResponse;
import com.huaweicloud.hdkitservice.model.CredentialsRequest;
import com.huaweicloud.hdkitservice.model.CredentialsResponse;
import com.huaweicloud.hdkitservice.model.SignAgreementResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Service
public class SandboxService {

    private static final Logger log = LoggerFactory.getLogger(SandboxService.class);
    private static final String STATUS_READY = "0004";
    private static final String STATUS_RUNNING = "0002";

    private final DevStationClient devStation;
    private final HdkitConfig config;

    public SandboxService(DevStationClient devStation, HdkitConfig config) {
        this.devStation = devStation;
        this.config = config;
    }

    public ConnectResponse connect(ConnectRequest req, String ak, String sk) {
        String templateId = (req.templateId() == null || req.templateId().isEmpty())
                ? config.templateId() : req.templateId();
        String flavorId = (req.flavorId() == null || req.flavorId().isEmpty())
                ? config.flavorId() : req.flavorId();

        List<DevStationClient.Devenv> actual = devStation.list("", ak, sk);

        DevStationClient.Devenv existing = findHcdkInstance(actual);
        String devStageId;
        boolean created = false;

        if (existing != null) {
            // 复用已有实例
            devStageId = existing.id();
        } else {
            // 按账号实时计数：云上现存环境数（含手动创建的）达到上限则拒绝
            if (actual.size() >= config.maxConcurrent()) {
                throw new HdkitException("HDKIT_CONFLICT", "已达最大并发沙箱数 " + config.maxConcurrent(), null);
            }
            // 新建实例（name 内部生成，保证唯一可识别）
            String name = "hcdk" + Long.toString(System.currentTimeMillis(), 36);
            devStageId = devStation.create(name, templateId, flavorId, req.source(), req.env(), req.git(), ak, sk);
            created = true;
        }

        try {
            if (created) {
                waitForStatus(devStageId, STATUS_READY, config.connectTimeout(), ak, sk);
            }
            ensureRunning(devStageId, ak, sk);
            devStation.autoConfig(devStageId, true, ak, sk); // 注入临时 AK/SK

            DevStationClient.Connections conns = devStation.connections(devStageId, config.source(), ak, sk);
            long connectionId = pickConnected(conns);
            DevStationClient.ConnectionAddress addr = devStation.address(devStageId, connectionId, ak, sk);
            String address = addr.url() + "&source=" + addr.source();

            // 无本地会话：session_id 等价 dev_stage_id
            return new ConnectResponse(devStageId, devStageId, String.valueOf(connectionId), address, "connected");
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

    private void ensureRunning(String devStageId, String ak, String sk) {
        String status = devStation.statusOf(devStageId, ak, sk);
        if (!isStatus(status, STATUS_RUNNING)) {
            devStation.start(devStageId, config.source(), ak, sk);
            waitForStatus(devStageId, STATUS_RUNNING, config.connectTimeout(), ak, sk);
        }
    }

    public CredentialsResponse credentials(CredentialsRequest req, String ak, String sk) {
        String devStageId = resolveDevStageId(req.sessionId(), req.devStageId());
        if (devStageId == null) {
            throw new HdkitException("HDKIT_INVALID_REQUEST", "缺少 session_id 或 dev_stage_id", null);
        }

        String status = devStation.statusOf(devStageId, ak, sk);
        if (status == null) {
            throw new HdkitException("HDKIT_SANDBOX_NOT_FOUND", "环境不存在或已被删除", null);
        }
        if (!isStatus(status, STATUS_RUNNING)) {
            throw new HdkitException("HDKIT_NOT_RUNNING", "环境未处于 RUNNING，无法注入临时 AK/SK", null);
        }

        boolean enableSts = req.enableSts() == null || req.enableSts();
        String expiresAt = devStation.autoConfig(devStageId, enableSts, ak, sk);

        return new CredentialsResponse(devStageId, expiresAt);
    }

    private void releaseById(String devStageId, String ak, String sk) {
        if (devStation.statusOf(devStageId, ak, sk) == null) {
            return; // 幂等：环境已不存在，视为已释放
        }
        devStation.close(devStageId, config.source(), ak, sk);
        waitForStatus(devStageId, STATUS_READY, config.releaseTimeout(), ak, sk);
        devStation.delete(devStageId, config.source(), ak, sk);
        waitForGone(devStageId, config.releaseTimeout(), ak, sk);
    }

    public CheckUserResponse checkUser(String ak, String sk) {
        Map<String, String> mdc = MDC.getCopyOfContextMap();
        CompletableFuture<Boolean> realnameFuture = CompletableFuture.supplyAsync(
                () -> withMdc(mdc, () -> "2".equals(devStation.realNameStatus(ak, sk))));
        CompletableFuture<Boolean> agreementFuture = CompletableFuture.supplyAsync(
                () -> withMdc(mdc, () -> allAgreementsSigned(devStation.agreements(ak, sk))));

        boolean realnameOk = await(realnameFuture, "查询实名状态失败");
        boolean agreementOk = await(agreementFuture, "查询协议状态失败");

        if (!realnameOk) throw new HdkitException("HDKIT_NOT_REALNAME", "用户未完成实名认证", null);
        if (!agreementOk) throw new HdkitException("HDKIT_NOT_AGREEMENT", "用户未签署协议", null);
        return new CheckUserResponse(true, true);
    }

    private static <T> T withMdc(Map<String, String> mdc, java.util.function.Supplier<T> supplier) {
        if (mdc != null) {
            MDC.setContextMap(mdc);
        }
        try {
            return supplier.get();
        } finally {
            MDC.clear();
        }
    }

    public SignAgreementResponse signAgreement(String ak, String sk) {
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
        // session_id 是 dev_stage_id 的别名（历史兼容）
        if (sessionId != null && !sessionId.isEmpty()) return sessionId;
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
