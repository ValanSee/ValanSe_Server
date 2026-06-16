package com.valanse.valanse.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "cloudflare.r2")
/**
 * 애플리케이션 인프라 설정을 담당하는 설정 코드입니다.
 */
public class R2Properties {

    private String accountId;
    private String endpoint;
    private String bucket;
    private String accessKey;
    private String secretKey;
    private String publicUrl;

    /**
     * AccountId 정보를 조회하는 메서드입니다.
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * R2Properties의 setAccountId 기능을 수행하는 메서드입니다.
     */
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    /**
     * Endpoint 정보를 조회하는 메서드입니다.
     */
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * R2Properties의 setEndpoint 기능을 수행하는 메서드입니다.
     */
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * Bucket 정보를 조회하는 메서드입니다.
     */
    public String getBucket() {
        return bucket;
    }

    /**
     * R2Properties의 setBucket 기능을 수행하는 메서드입니다.
     */
    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    /**
     * AccessKey 정보를 조회하는 메서드입니다.
     */
    public String getAccessKey() {
        return accessKey;
    }

    /**
     * R2Properties의 setAccessKey 기능을 수행하는 메서드입니다.
     */
    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    /**
     * SecretKey 정보를 조회하는 메서드입니다.
     */
    public String getSecretKey() {
        return secretKey;
    }

    /**
     * R2Properties의 setSecretKey 기능을 수행하는 메서드입니다.
     */
    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    /**
     * PublicUrl 정보를 조회하는 메서드입니다.
     */
    public String getPublicUrl() {
        return publicUrl;
    }

    /**
     * R2Properties의 setPublicUrl 기능을 수행하는 메서드입니다.
     */
    public void setPublicUrl(String publicUrl) {
        this.publicUrl = publicUrl;
    }
}
