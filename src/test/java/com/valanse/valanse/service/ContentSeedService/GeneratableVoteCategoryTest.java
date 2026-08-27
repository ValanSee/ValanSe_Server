package com.valanse.valanse.service.ContentSeedService;

import com.valanse.valanse.domain.enums.VoteCategory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeneratableVoteCategoryTest {

    @Test
    void 모든_값이_동일한_이름의_VoteCategory로_변환된다() {
        for (GeneratableVoteCategory category : GeneratableVoteCategory.values()) {
            assertThat(category.toVoteCategory().name()).isEqualTo(category.name());
        }
    }

    @Test
    void ALL은_애초에_값으로_존재하지_않는다() {
        assertThat(GeneratableVoteCategory.values())
                .extracting(Enum::name)
                .doesNotContain(VoteCategory.ALL.name());
    }
}
