package com.huaweicloud.hdkitservice.controller;

import com.huaweicloud.hdkitservice.model.ErrorResponse;
import com.huaweicloud.hdkitservice.service.SandboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final Map<String, HttpStatus> STATUS_MAP = Map.ofEntries(
            Map.entry("HDKIT_INVALID_REQUEST", HttpStatus.BAD_REQUEST),
            Map.entry("HDKIT_NOT_RUNNING", HttpStatus.UNPROCESSABLE_ENTITY),
            Map.entry("HDKIT_CONFLICT", HttpStatus.CONFLICT),
            Map.entry("HDKIT_NOT_REALNAME", HttpStatus.FORBIDDEN),
            Map.entry("HDKIT_NOT_REALNAME_AND_AGREEMENT", HttpStatus.FORBIDDEN),
            Map.entry("HDKIT_NOT_AGREEMENT", HttpStatus.FORBIDDEN),
            Map.entry("HDKIT_SANDBOX_NOT_FOUND", HttpStatus.NOT_FOUND),
            Map.entry("HDKIT_TIMEOUT", HttpStatus.GATEWAY_TIMEOUT),
            Map.entry("HDKIT_RELEASE_TIMEOUT", HttpStatus.GATEWAY_TIMEOUT),
            Map.entry("HDKIT_UPSTREAM_ERROR", HttpStatus.BAD_GATEWAY),
            Map.entry("HDKIT_CONNECT_FAILED", HttpStatus.BAD_GATEWAY),
            Map.entry("HDKIT_RELEASE_FAILED", HttpStatus.BAD_GATEWAY));

    @ExceptionHandler(SandboxService.HdkitException.class)
    public ResponseEntity<ErrorResponse> handle(SandboxService.HdkitException e) {
        log.error("[error] {}: {}", e.code(), e.getMessage());
        HttpStatus status = STATUS_MAP.getOrDefault(e.code(), HttpStatus.INTERNAL_SERVER_ERROR);
        return ResponseEntity.status(status)
                .body(new ErrorResponse(e.code(), e.getMessage(), traceId()));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("HDKIT_INVALID_REQUEST", "缺少请求头 " + e.getHeaderName(),
                        traceId()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOther(Exception e) {
        log.error("[error] unexpected: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("HDKIT_INTERNAL", "服务内部错误", traceId()));
    }

    private String traceId() {
        String requestId = MDC.get("requestId");
        return requestId != null ? requestId : UUID.randomUUID().toString();
    }
}
