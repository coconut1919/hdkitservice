package com.huaweicloud.hdkitservice.controller;

import com.huaweicloud.hdkitservice.model.CheckUserResponse;
import com.huaweicloud.hdkitservice.model.ConnectRequest;
import com.huaweicloud.hdkitservice.model.ConnectResponse;
import com.huaweicloud.hdkitservice.model.CredentialsRequest;
import com.huaweicloud.hdkitservice.model.CredentialsResponse;
import com.huaweicloud.hdkitservice.model.SignAgreementResponse;
import com.huaweicloud.hdkitservice.service.SandboxService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/developer/server/hdkitservice")
public class SandboxController {

    private final SandboxService service;

    public SandboxController(SandboxService service) {
        this.service = service;
    }

    @GetMapping("/check-user")
    public CheckUserResponse checkUser(@RequestHeader("X-HW-AK") String ak,
                                       @RequestHeader("X-HW-SK") String sk) {
        return service.checkUser(ak, sk);
    }

    @PostMapping("/sign-agreement")
    public SignAgreementResponse signAgreement(@RequestHeader("X-HW-AK") String ak,
                                               @RequestHeader("X-HW-SK") String sk) {
        return service.signAgreement(ak, sk);
    }

    @PostMapping("/connect")
    public ConnectResponse connect(@RequestBody ConnectRequest req,
                                   @RequestHeader("X-HW-AK") String ak,
                                   @RequestHeader("X-HW-SK") String sk) {
        return service.connect(req, ak, sk);
    }

    @PostMapping("/credentials")
    public CredentialsResponse credentials(@RequestBody CredentialsRequest req,
                                           @RequestHeader("X-HW-AK") String ak,
                                           @RequestHeader("X-HW-SK") String sk) {
        return service.credentials(req, ak, sk);
    }
}
