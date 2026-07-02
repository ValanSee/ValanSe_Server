package com.valanse.valanse.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class MultipartConfigurationTest {

    @Autowired
    private MultipartProperties multipartProperties;

    @Test
    @DisplayName("multipart는 파일당 5MB, 요청당 25MB로 제한한다")
    void multipartLimits_AreConfigured() {
        assertThat(multipartProperties.getMaxFileSize().toBytes()).isEqualTo(5L * 1024 * 1024);
        assertThat(multipartProperties.getMaxRequestSize().toBytes()).isEqualTo(25L * 1024 * 1024);
    }
}
