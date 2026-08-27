package com.huaweicloud.hdkitservice.controller;

import com.huaweicloud.hdkitservice.model.TelemetryEventDto;
import com.huaweicloud.hdkitservice.service.TelemetryService;
import com.huaweicloud.hdkitservice.util.Masker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TelemetryController.class)
class TelemetryControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private TelemetryService telemetryService;

    @MockBean
    private Masker masker;

    @Test
    void postEventsReturnsReceived() throws Exception {
        when(telemetryService.saveBatch(anyList())).thenReturn(3);

        mvc.perform(post("/rest/developer/server/hdkitservice/telemetry/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                    {"key":"event1","value":"v1","installId":"inst1"},
                                    {"key":"event2","value":"v2","installId":"inst2"},
                                    {"key":"event3","value":"v3","installId":"inst3"}
                                ]"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.received").value(3));
    }

    @Test
    void postEventsEmptyListReturnsZero() throws Exception {
        mvc.perform(post("/rest/developer/server/hdkitservice/telemetry/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.received").value(0));
    }

    @Test
    void postEventsWithoutInstallIdReturns400() throws Exception {
        mvc.perform(post("/rest/developer/server/hdkitservice/telemetry/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [{"key":"event1","value":"v1"}]"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("HDKIT_INVALID_REQUEST"));
    }

    @Test
    void postEventsWithBlankInstallIdReturns400() throws Exception {
        mvc.perform(post("/rest/developer/server/hdkitservice/telemetry/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [{"key":"event1","value":"v1","installId":"  "}]"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("HDKIT_INVALID_REQUEST"));
    }
}