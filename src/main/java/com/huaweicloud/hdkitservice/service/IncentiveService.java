package com.huaweicloud.hdkitservice.service;

import com.huaweicloud.hdkitservice.config.HdkitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class IncentiveService {

    private static final Logger log = LoggerFactory.getLogger(IncentiveService.class);

    private final IncentiveClient client;
    private final HdkitConfig config;

    public IncentiveService(IncentiveClient client, HdkitConfig config) {
        this.client = client;
        this.config = config;
    }

    public VoucherStatusResult voucherStatus(String ak, String sk) {
        String domainId = client.resolveDomainId(ak, sk);
        IncentiveClient.CheckResult check = client.checkCouponIssued(domainId);

        if (check.serviceError()) {
            return new VoucherStatusResult(false, "查询失败");
        }
        if (check.issued()) {
            return new VoucherStatusResult(true, "已领取");
        }
        return new VoucherStatusResult(false, "未领取");
    }

    public VoucherClaimResult voucherClaim(String ak, String sk) {
        String domainId = client.resolveDomainId(ak, sk);

        IncentiveClient.CheckResult check = client.checkCouponIssued(domainId);
        if (check.serviceError()) {
            throw new SandboxService.HdkitException("HDKIT_INCENTIVE_ERROR",
                    "激励服务查询失败，请稍后重试", null);
        }
        if (check.issued()) {
            return new VoucherClaimResult(true, null, 0, "已领取过");
        }

        IncentiveClient.IssueResult issue = client.issueCoupon(domainId);
        if (!issue.success()) {
            if ("HD.60620016".equals(issue.errorCode())) {
                return new VoucherClaimResult(true, null, 0, "已领取过");
            }
            if ("HD.60630042".equals(issue.errorCode())) {
                throw new SandboxService.HdkitException("HDKIT_INCENTIVE_ERROR",
                        "本月代金券总额度已用完，请下月再试", null);
            }
            String errMsg = "发券失败: " + issue.error();
            if ("HD.60630022".equals(issue.errorCode())) {
                errMsg += " 请先完成实名认证：https://account.huaweicloud.com/usercenter/"
                        + "?region=cn-north-4&locale=zh-cn#/accountindex/realNameAuthing";
            }
            throw new SandboxService.HdkitException("HDKIT_INCENTIVE_ERROR", errMsg, null);
        }

        int amount = Math.min(config.incentiveFaceAmount(), 500);
        return new VoucherClaimResult(true, issue.couponId(), amount, "领取成功");
    }

    public record VoucherStatusResult(boolean claimed, String message) {}
    public record VoucherClaimResult(boolean claimed, String voucherId, int amount, String message) {}
}