package com.valanse.valanse.service.PurgeService;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "purge")
public class PurgeProperties {
    private boolean enabled = true;
    private int retentionDays = 14;
    private int batchSize = 100;

    public PurgeProperties() {
    }

    public PurgeProperties(boolean enabled, int retentionDays, int batchSize) {
        this.enabled = enabled;
        this.retentionDays = retentionDays;
        this.batchSize = batchSize;
    }

    public boolean enabled() { return enabled; }
    public int retentionDays() { return retentionDays; }
    public int batchSize() { return batchSize; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setRetentionDays(int retentionDays) { this.retentionDays = retentionDays; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
}
