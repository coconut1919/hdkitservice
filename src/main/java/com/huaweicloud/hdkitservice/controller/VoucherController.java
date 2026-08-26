package com.huaweicloud.hdkitservice.controller;

import com.huaweicloud.hdkitservice.model.VoucherStatusResponse;
import com.huaweicloud.hdkitservice.model.VoucherClaimResponse;
import com.huaweicloud.hdkitservice.service.IncentiveService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/developer/server/hdkitservice")
public class VoucherController {

    private final IncentiveService incentiveService;

    public VoucherController(IncentiveService incentiveService) {
        this.incentiveService = incentiveService;
    }

    @GetMapping("/voucher/status")
    public VoucherStatusResponse voucherStatus(@RequestHeader("X-HW-AK") String ak,
                                               @RequestHeader("X-HW-SK") String sk) {
        var result = incentiveService.voucherStatus(ak, sk);
        return new VoucherStatusResponse(result.claimed(), result.message());
    }

    @PostMapping("/voucher/claim")
    public VoucherClaimResponse voucherClaim(@RequestHeader("X-HW-AK") String ak,
                                             @RequestHeader("X-HW-SK") String sk) {
        var result = incentiveService.voucherClaim(ak, sk);
        return new VoucherClaimResponse(result.claimed(), result.voucherId(), result.amount(), result.message());
    }
}