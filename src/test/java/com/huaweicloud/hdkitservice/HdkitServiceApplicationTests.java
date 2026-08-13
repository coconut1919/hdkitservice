package com.huaweicloud.hdkitservice;

import com.huaweicloud.hdkitservice.service.SandboxService;
import com.huaweicloud.hdkitservice.store.SessionStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class HdkitServiceApplicationTests {

    @MockBean
    private SandboxService sandboxService;

    @MockBean
    private SessionStore sessionStore;

    @Test
    void contextLoads() {
    }
}
