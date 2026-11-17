package com.valanse.valanse.service.SearchKeywordService;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KeywordNormalizerTest {

    @Test
    void normalize_removesWhitespaceAndLowercases() {
        assertThat(KeywordNormalizer.normalize("커플 밸런스게임 ")).isEqualTo("커플밸런스게임");
        assertThat(KeywordNormalizer.normalize("ValanSe")).isEqualTo("valanse");
        assertThat(KeywordNormalizer.normalize(null)).isEmpty();
    }

    @Test
    void core_removesGameTokens() {
        assertThat(KeywordNormalizer.core("커플 밸런스게임")).isEqualTo("커플");
        assertThat(KeywordNormalizer.core("부먹 vs 찍먹")).isEqualTo("부먹찍먹");
    }

    @Test
    void overlaps_notContained_false() {
        assertThat(KeywordNormalizer.overlaps("라면면먼저스프먼저밸런스게임", "라면 끓일 때 면 먼저 vs 스프 먼저")).isFalse();
    }

    @Test
    void overlaps_contained_true() {
        assertThat(KeywordNormalizer.overlaps("부먹찍먹", "탕수육 부먹 vs 찍먹")).isTrue();
    }

    @Test
    void overlaps_shortKeywordCore_false() {
        assertThat(KeywordNormalizer.overlaps("커플밸런스게임", "커플 여행 계획")).isFalse();
    }

    @Test
    void overlaps_emptyCore_false() {
        assertThat(KeywordNormalizer.overlaps("밸런스게임", "아무 제목")).isFalse();
        assertThat(KeywordNormalizer.overlaps("부먹찍먹", "밸런스게임")).isFalse();
    }

    @Test
    void containsBrand() {
        assertThat(KeywordNormalizer.containsBrand("발란세 앱", List.of("발란세"))).isTrue();
        assertThat(KeywordNormalizer.containsBrand("벨런스게임", List.of("발란세", "valanse", "valsense", "valance"))).isFalse();
    }
}
