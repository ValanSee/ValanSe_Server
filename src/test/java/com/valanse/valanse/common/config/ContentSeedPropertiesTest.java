package com.valanse.valanse.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class ContentSeedPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    void 기본값만으로도_바인딩과_검증을_통과한다() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            ContentSeedProperties properties = context.getBean(ContentSeedProperties.class);
            assertThat(properties.isEnabled()).isFalse();
            assertThat(properties.getModel()).isEqualTo("claude-sonnet-5");
            assertThat(properties.getBotsPerRun()).isEqualTo(2);
        });
    }

    @Test
    void botsPerRun이_0이면_검증에_실패한다() {
        contextRunner.withPropertyValues("content-seed.bots-per-run=0")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void botsPerRun이_시드된_봇_수를_초과하면_검증에_실패한다() {
        contextRunner.withPropertyValues("content-seed.bots-per-run=" + (ContentSeedProperties.SEEDED_BOT_COUNT + 1))
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void botsPerRun이_시드된_봇_수와_같으면_통과한다() {
        contextRunner.withPropertyValues("content-seed.bots-per-run=" + ContentSeedProperties.SEEDED_BOT_COUNT)
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void postsPerBot이_0이면_검증에_실패한다() {
        contextRunner.withPropertyValues("content-seed.posts-per-bot=0")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void model이_비어있으면_검증에_실패한다() {
        contextRunner.withPropertyValues("content-seed.model=")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void commentContextLimit은_0을_허용한다() {
        contextRunner.withPropertyValues("content-seed.comment-context-limit=0")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void 토큰_단가가_0_이하이면_검증에_실패한다() {
        contextRunner.withPropertyValues("content-seed.pricing.input-token-price-per-million-usd=0")
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration
    @EnableConfigurationProperties(ContentSeedProperties.class)
    static class TestConfig {
    }
}
