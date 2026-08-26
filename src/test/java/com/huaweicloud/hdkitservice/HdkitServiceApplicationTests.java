package com.huaweicloud.hdkitservice;

import com.huaweicloud.hdkitservice.service.IncentiveClient;
import com.huaweicloud.hdkitservice.service.SandboxService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class HdkitServiceApplicationTests {

    @MockBean
    private SandboxService sandboxService;

    @MockBean
    private IncentiveClient incentiveClient;

    @Test
    void contextLoads() {
    }
}
