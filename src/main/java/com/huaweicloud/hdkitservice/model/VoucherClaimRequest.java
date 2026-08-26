package com.huaweicloud.hdkitservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VoucherClaimRequest(
        @JsonProperty("domain_id") String domainId) {}