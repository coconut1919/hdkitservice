package com.huaweicloud.hdkitservice.model;

public record VoucherClaimResponse(boolean claimed, String voucherId, int amount, String message) {}