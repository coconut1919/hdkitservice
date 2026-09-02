package com.huaweicloud.hdkitservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.huaweicloud.hdkitservice.config.DashboardConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class NpmDownloadClient {

    private static final Logger log = LoggerFactory.getLogger(NpmDownloadClient.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final RestClient restClient;
    private final DashboardConfig config;

    public NpmDownloadClient(RestClient hdkitRestClient, DashboardConfig config) {
        this.restClient = hdkitRestClient;
        this.config = config;
    }

    public long fetchDailyDownloads(LocalDate date) {
        String url = config.npmApiBase() + "/downloads/point/" + date.format(FMT) + "/" + config.npmPackageName();
        try {
            JsonNode node = restClient.get().uri(url).retrieve().body(JsonNode.class);
            if (node != null && node.has("downloads")) {
                return node.get("downloads").asLong();
            }
        } catch (Exception e) {
            log.warn("[npm] fetch daily downloads failed for {}: {}", date, e.getMessage());
        }
        return 0;
    }

    public long fetchWeekDownloads() {
        String url = config.npmApiBase() + "/downloads/point/last-week/" + config.npmPackageName();
        try {
            JsonNode node = restClient.get().uri(url).retrieve().body(JsonNode.class);
            if (node != null && node.has("downloads")) {
                return node.get("downloads").asLong();
            }
        } catch (Exception e) {
            log.warn("[npm] fetch week downloads failed: {}", e.getMessage());
        }
        return 0;
    }

    public long fetchCumulativeDownloads(LocalDate publishDate, LocalDate endDate) {
        long total = 0;
        LocalDate cursor = publishDate;
        int segmentLimit = 548;

        while (cursor.isBefore(endDate)) {
            LocalDate segEnd = cursor.plusDays(segmentLimit);
            if (segEnd.isAfter(endDate)) {
                segEnd = endDate;
            }
            String range = cursor.format(FMT) + ":" + segEnd.format(FMT);
            String url = config.npmApiBase() + "/downloads/point/" + range + "/" + config.npmPackageName();
            try {
                JsonNode node = restClient.get().uri(url).retrieve().body(JsonNode.class);
                if (node != null && node.has("downloads")) {
                    total += node.get("downloads").asLong();
                }
            } catch (Exception e) {
                log.warn("[npm] fetch cumulative segment {} failed: {}", range, e.getMessage());
            }
            cursor = segEnd.plusDays(1);
        }
        return total;
    }
}
