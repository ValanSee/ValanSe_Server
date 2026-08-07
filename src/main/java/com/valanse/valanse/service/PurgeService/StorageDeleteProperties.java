package com.valanse.valanse.service.PurgeService;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "purge.storage-delete")
public class StorageDeleteProperties {
    private boolean enabled = true;
    private int batchSize = 50;
    private int maxAttempts = 8;
    private long leaseSeconds = 300;
    private long initialRetrySeconds = 30;
    private long maxRetrySeconds = 3600;

    public boolean enabled() {
        return enabled;
    }

    public int batchSize() {
        return batchSize;
    }

    public int maxAttempts() {
        return maxAttempts;
    }

    public long leaseSeconds() {
        return leaseSeconds;
    }

    public long initialRetrySeconds() {
        return initialRetrySeconds;
    }

    public long maxRetrySeconds() {
        return maxRetrySeconds;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public void setLeaseSeconds(long leaseSeconds) {
        this.leaseSeconds = leaseSeconds;
    }

    public void setInitialRetrySeconds(long initialRetrySeconds) {
        this.initialRetrySeconds = initialRetrySeconds;
    }

    public void setMaxRetrySeconds(long maxRetrySeconds) {
        this.maxRetrySeconds = maxRetrySeconds;
    }
}
