package com.huaweicloud.hdkitservice.service;

import com.huaweicloud.hdkitservice.store.SessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReclaimTask {

    private static final Logger log = LoggerFactory.getLogger(ReclaimTask.class);

    private final SessionStore store;

    public ReclaimTask(SessionStore store) {
        this.store = store;
    }

    @Scheduled(fixedDelayString = "${RECLAIM_INTERVAL:300000}")
    public void reclaim() {
        log.info("[reclaim] pruning active set of expired sessions");
        store.pruneActive();
    }
}
