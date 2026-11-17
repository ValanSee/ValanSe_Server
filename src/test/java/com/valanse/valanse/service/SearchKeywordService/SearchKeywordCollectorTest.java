package com.valanse.valanse.service.SearchKeywordService;

import com.valanse.valanse.domain.SearchKeyword;
import com.valanse.valanse.domain.enums.SearchKeywordSource;
import com.valanse.valanse.repository.SearchKeywordRepository;
import com.valanse.valanse.repository.SearchKeywordSnapshotRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class SearchKeywordCollectorTest {

    @Autowired private SearchKeywordCollector collector;
    @Autowired private SearchKeywordRepository keywordRepository;
    @Autowired private SearchKeywordSnapshotRepository snapshotRepository;
    @MockBean private NaverSearchAdKeywordClient naverClient;
    @MockBean private GoogleSearchConsoleClient googleClient;

    @BeforeEach
    void clean() {
        snapshotRepository.deleteAll();
        keywordRepository.deleteAll();
    }

    @AfterEach
    void cleanAfter() {
        clean();
    }

    private static CollectedKeyword kw(String keyword, Integer volume) {
        return new CollectedKeyword(keyword, volume, null, null, null);
    }

    @Test
    void skips_unconfigured_sources_without_http() {
        when(naverClient.isConfigured()).thenReturn(false);
        when(googleClient.isConfigured()).thenReturn(false);

        SearchKeywordCollectResult result = collector.collect();

        verify(naverClient, never()).fetch();
        verify(googleClient, never()).fetch();
        assertThat(result.savedCounts().get(SearchKeywordSource.NAVER_SEARCH_AD)).isZero();
        assertThat(result.savedCounts().get(SearchKeywordSource.GOOGLE_SEARCH_CONSOLE)).isZero();
        assertThat(result.failures()).isEmpty();
    }

    @Test
    void one_source_failure_does_not_block_other() {
        when(naverClient.isConfigured()).thenReturn(true);
        when(naverClient.fetch()).thenThrow(new IllegalStateException("naver down"));
        when(googleClient.isConfigured()).thenReturn(true);
        when(googleClient.fetch()).thenReturn(List.of(kw("커플 게임", 10), kw("술자리 게임", 20)));

        SearchKeywordCollectResult result = collector.collect();

        assertThat(result.savedCounts().get(SearchKeywordSource.GOOGLE_SEARCH_CONSOLE)).isEqualTo(2);
        assertThat(result.failures()).containsOnlyKeys(SearchKeywordSource.NAVER_SEARCH_AD);
        assertThat(keywordRepository.count()).isEqualTo(2);
    }

    @Test
    void upserts_master_and_overwrites_same_day_snapshot() {
        when(naverClient.isConfigured()).thenReturn(true);
        when(googleClient.isConfigured()).thenReturn(false);
        when(naverClient.fetch()).thenReturn(List.of(kw("커플 밸런스게임", 100)));
        collector.collect();
        when(naverClient.fetch()).thenReturn(List.of(kw("커플 밸런스게임", 200)));
        collector.collect();

        List<SearchKeyword> keywords = keywordRepository.findAll();
        assertThat(keywords).hasSize(1);
        assertThat(keywords.get(0).getMonthlySearchVolume()).isEqualTo(200);
        assertThat(snapshotRepository.findAll()).hasSize(1);
        assertThat(snapshotRepository.findAll().get(0).getMonthlySearchVolume()).isEqualTo(200);
    }

    @Test
    void dedupes_by_normalized_keyword() {
        when(naverClient.isConfigured()).thenReturn(true);
        when(googleClient.isConfigured()).thenReturn(false);
        when(naverClient.fetch()).thenReturn(List.of(kw("커플 밸런스게임", 1), kw("커플밸런스게임", 2)));

        SearchKeywordCollectResult result = collector.collect();

        assertThat(keywordRepository.findAll()).hasSize(1);
        assertThat(keywordRepository.findAll().get(0).getKeyword()).isEqualTo("커플 밸런스게임");
        assertThat(result.savedCounts().get(SearchKeywordSource.NAVER_SEARCH_AD)).isEqualTo(1);
    }
}
