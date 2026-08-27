package com.valanse.valanse.common.config;

import com.anthropic.client.AnthropicClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AnthropicConfigTest {

    @Autowired
    private AnthropicClient anthropicClient;

    @Test
    void 애플리케이션_컨텍스트는_API_Key_환경변수_없이도_기동된다() {
        assertThat(anthropicClient).isNotNull();
    }
}
