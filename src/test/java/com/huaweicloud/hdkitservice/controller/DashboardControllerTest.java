package com.huaweicloud.hdkitservice.controller;

import com.huaweicloud.hdkitservice.model.AgentDistributionDTO;
import com.huaweicloud.hdkitservice.model.DeveloperSummaryDTO;
import com.huaweicloud.hdkitservice.model.DownloadSummaryDTO;
import com.huaweicloud.hdkitservice.model.DownloadTrendDTO;
import com.huaweicloud.hdkitservice.service.DashboardService;
import com.huaweicloud.hdkitservice.util.Masker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private Masker masker;

    @Test
    void developerSummaryReturnsOk() throws Exception {
        DeveloperSummaryDTO dto = new DeveloperSummaryDTO(
                1000, 50, 12.5, 200, 800, 15,
                List.of(new DeveloperSummaryDTO.DailyTrendPoint("2026-09-01", 200))
        );
        when(dashboardService.getDeveloperSummary()).thenReturn(dto);

        mvc.perform(get("/rest/developer/server/hdkitservice/dashboard/developer/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDevelopers").value(1000))
                .andExpect(jsonPath("$.dau").value(200))
                .andExpect(jsonPath("$.mau").value(800))
                .andExpect(jsonPath("$.agentTotal").value(15));
    }

    @Test
    void agentDistributionReturnsOk() throws Exception {
        AgentDistributionDTO dto = new AgentDistributionDTO(
                "2026-09-01", 100,
                List.of(new AgentDistributionDTO.AgentItem("opencode", 50, 50.0))
        );
        when(dashboardService.getAgentDistribution()).thenReturn(dto);

        mvc.perform(get("/rest/developer/server/hdkitservice/dashboard/agent/distribution"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agents[0].name").value("opencode"))
                .andExpect(jsonPath("$.agents[0].count").value(50));
    }

    @Test
    void downloadSummaryReturnsOk() throws Exception {
        DownloadSummaryDTO dto = new DownloadSummaryDTO(478, 2322, 10410, "@huaweicloud/huaweicloud-devkit", "2026-09-01");
        when(dashboardService.getDownloadSummary()).thenReturn(dto);

        mvc.perform(get("/rest/developer/server/hdkitservice/dashboard/download/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.npmToday").value(478))
                .andExpect(jsonPath("$.npmCumulative").value(10410));
    }

    @Test
    void downloadTrendReturnsOk() throws Exception {
        DownloadTrendDTO dto = new DownloadTrendDTO(
                List.of(new DownloadTrendDTO.TrendPoint("2026-09-01", 478)), 478
        );
        when(dashboardService.getDownloadTrend()).thenReturn(dto);

        mvc.perform(get("/rest/developer/server/hdkitservice/dashboard/download/trend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.npmDaily[0].downloads").value(478));
    }

    @Test
    void aggregateReturnsOk() throws Exception {
        mvc.perform(get("/rest/developer/server/hdkitservice/dashboard/aggregate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }
}
