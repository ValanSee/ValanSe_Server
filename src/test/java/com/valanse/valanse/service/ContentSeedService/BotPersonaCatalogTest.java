package com.valanse.valanse.service.ContentSeedService;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * REQ-001: 봇 관심사·말투 힌트는 닉네임이 아니라 안정적인 social_id로 조회되어야
 * 한다. 이 테스트는 BotPersonaCatalog가 social_id 기준으로 일관된 값을 반환하고,
 * 알 수 없는/null social_id에 대해서는 안전한 기본값을 반환하는지 검증한다.
 */
class BotPersonaCatalogTest {

    @Test
    void 알려진_봇_social_id는_관심사와_말투_힌트를_반환한다() {
        BotPersonaCatalog.BotPersonaTraits traits = BotPersonaCatalog.traitsFor("content-seed-bot-001");

        assertThat(traits.interestHint()).isNotBlank();
        assertThat(traits.toneExample()).isNotBlank();
    }

    @Test
    void 다섯_봇_모두_서로_다른_관심사_힌트를_가진다() {
        var ids = java.util.List.of(
                "content-seed-bot-001", "content-seed-bot-002", "content-seed-bot-003",
                "content-seed-bot-004", "content-seed-bot-005");

        var interestHints = ids.stream()
                .map(id -> BotPersonaCatalog.traitsFor(id).interestHint())
                .distinct()
                .toList();

        assertThat(interestHints).hasSize(5);
    }

    @Test
    void 알_수_없는_social_id는_빈_기본값을_반환한다() {
        BotPersonaCatalog.BotPersonaTraits traits = BotPersonaCatalog.traitsFor("unknown-social-id");

        assertThat(traits.interestHint()).isEmpty();
        assertThat(traits.toneExample()).isEmpty();
    }

    @Test
    void null_social_id는_예외_없이_빈_기본값을_반환한다() {
        BotPersonaCatalog.BotPersonaTraits traits = BotPersonaCatalog.traitsFor(null);

        assertThat(traits.interestHint()).isEmpty();
        assertThat(traits.toneExample()).isEmpty();
    }
}
