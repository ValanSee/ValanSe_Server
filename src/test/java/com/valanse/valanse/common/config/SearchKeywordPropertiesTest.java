package com.valanse.valanse.common.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SearchKeywordPropertiesTest {

    @Test
    void 기본값은_켜져_있다() {
        SearchKeywordProperties properties = new SearchKeywordProperties();

        assertThat(properties.isEnabled()).isTrue();
    }

    @Test
    void 네이버_힌트_키워드_기본값은_spec과_같다() {
        SearchKeywordProperties properties = new SearchKeywordProperties();

        assertThat(properties.getNaver().getHintKeywords()).containsExactly(
                "밸런스게임", "커플밸런스게임", "연애밸런스게임", "음식밸런스게임", "친구밸런스게임");
    }

    @Test
    void 브랜드_용어_기본값은_spec과_같다() {
        SearchKeywordProperties properties = new SearchKeywordProperties();

        assertThat(properties.getBrandTerms()).containsExactly("발란세", "valanse", "valsense", "valance");
    }
}
