package com.huaweicloud.hdkitservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient hdkitRestClient(CallLogInterceptor callLogInterceptor) {
        return RestClient.builder()
                .requestInterceptor(callLogInterceptor)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}