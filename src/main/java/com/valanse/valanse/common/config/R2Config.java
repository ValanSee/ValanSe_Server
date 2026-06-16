package com.valanse.valanse.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
/**
 * 애플리케이션 인프라 설정을 담당하는 설정 코드입니다.
 */
public class R2Config {

    /**
     * R2Config의 s3Client 기능을 수행하는 메서드입니다.
     */
    @Bean
    public S3Client s3Client(R2Properties properties) {
        return S3Client.builder()
                .region(Region.of("auto"))
                .endpointOverride(URI.create(properties.getEndpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())
                ))
                .forcePathStyle(true)
                .build();
    }
}
