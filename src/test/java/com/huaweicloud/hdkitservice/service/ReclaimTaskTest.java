package com.huaweicloud.hdkitservice.service;

import com.huaweicloud.hdkitservice.store.SessionStore;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ReclaimTaskTest {

    @Test
    void reclaimPrunesActiveSet() {
        SessionStore store = mock(SessionStore.class);
        ReclaimTask task = new ReclaimTask(store);

        task.reclaim();

        verify(store).pruneActive();
    }
}
