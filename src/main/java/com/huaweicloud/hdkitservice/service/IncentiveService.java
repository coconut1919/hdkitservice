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

    public VoucherStatusResult voucherStatus(String ak, String sk, String securityToken, String providedDomainId) {
        if ("test".equalsIgnoreCase(config.deployEnv())
                && (providedDomainId == null || providedDomainId.isEmpty())) {
            return new VoucherStatusResult(false, "测试环境需提供 domain_id");
        }
        String domainId;
        try {
            domainId = "test".equalsIgnoreCase(config.deployEnv())
                    ? providedDomainId : client.resolveDomainIdFromIam(ak, sk, securityToken);
        } catch (IncentiveClient.IncentiveException e) {
            return new VoucherStatusResult(false, "查询失败");
        }

        IncentiveClient.CheckResult check = client.checkCouponIssued(domainId);
        if (check.serviceError()) {
            return new VoucherStatusResult(false, "查询失败");
        }
        if (check.issued()) {
            return new VoucherStatusResult(true, "已领取");
        }
        return new VoucherStatusResult(false, "未领取");
    }

    public VoucherClaimResult voucherClaim(String ak, String sk, String securityToken, String providedDomainId) {
        if ("test".equalsIgnoreCase(config.deployEnv())
                && (providedDomainId == null || providedDomainId.isEmpty())) {
            return new VoucherClaimResult(false, null, 0, "测试环境需提供 domain_id");
        }
        String domainId;
        try {
            domainId = "test".equalsIgnoreCase(config.deployEnv())
                    ? providedDomainId : client.resolveDomainIdFromIam(ak, sk, securityToken);
        } catch (IncentiveClient.IncentiveException e) {
            return new VoucherClaimResult(false, null, 0, "激励服务查询失败，请稍后重试");
        }

        IncentiveClient.CheckResult check = client.checkCouponIssued(domainId);
        if (check.serviceError()) {
            return new VoucherClaimResult(false, null, 0, "激励服务查询失败，请稍后重试");
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
                return new VoucherClaimResult(false, null, 0,
                        "本月代金券总额度已用完，所有账号均无法领取，请下月再重试");
            }
            String errMsg = "发券失败: " + issue.error();
            if ("HD.60630022".equals(issue.errorCode())) {
                errMsg += " 请先完成实名认证：https://account.huaweicloud.com/usercenter/"
                        + "?region=cn-north-4&locale=zh-cn#/accountindex/realNameAuthing";
            }
            return new VoucherClaimResult(false, null, 0, errMsg);
        }

        int amount = Math.min(config.incentiveFaceAmount(), 500);
        return new VoucherClaimResult(true, issue.couponId(), amount, "领取成功");
    }

    public record VoucherStatusResult(boolean claimed, String message) {}
    public record VoucherClaimResult(boolean claimed, String voucherId, int amount, String message) {}
}