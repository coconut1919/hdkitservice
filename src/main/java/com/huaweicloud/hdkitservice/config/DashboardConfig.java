package com.huaweicloud.hdkitservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DashboardConfig {

    @Value("${DASHBOARD_NPM_PACKAGE_NAME:@huaweicloud/huaweicloud-devkit}")
    private String npmPackageName;

    @Value("${DASHBOARD_NPM_PUBLISH_DATE:2025-01-01}")
    private String npmPublishDate;

    @Value("${DASHBOARD_NPM_API_BASE:https://api.npmjs.org}")
    private String npmApiBase;

    @Value("${DASHBOARD_AGGREGATION_ENABLED:true}")
    private boolean aggregationEnabled;

    @Value("${DASHBOARD_NPM_FETCH_ENABLED:true}")
    private boolean npmFetchEnabled;

    public String npmPackageName() { return npmPackageName; }
    public String npmPublishDate() { return npmPublishDate; }
    public String npmApiBase() { return npmApiBase; }
    public boolean aggregationEnabled() { return aggregationEnabled; }
    public boolean npmFetchEnabled() { return npmFetchEnabled; }

    public void setNpmPackageName(String v) { this.npmPackageName = v; }
    public void setNpmPublishDate(String v) { this.npmPublishDate = v; }
    public void setNpmApiBase(String v) { this.npmApiBase = v; }
    public void setAggregationEnabled(boolean v) { this.aggregationEnabled = v; }
    public void setNpmFetchEnabled(boolean v) { this.npmFetchEnabled = v; }
}
