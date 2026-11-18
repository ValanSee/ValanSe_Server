package com.valanse.valanse.service.SearchKeywordService;

import com.valanse.valanse.common.config.SearchKeywordProperties;
import com.valanse.valanse.domain.SearchKeyword;
import com.valanse.valanse.domain.enums.SearchKeywordSource;
import com.valanse.valanse.repository.SearchKeywordRepository;
import com.valanse.valanse.repository.VoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SearchKeywordSelectorTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final Instant INSTANT = Instant.parse("2026-11-16T00:00:00Z");
    private static final LocalDateTime NOW = LocalDateTime.ofInstant(INSTANT, ZONE);

    private SearchKeywordRepository keywordRepository;
    private VoteRepository voteRepository;
    private SearchKeywordProperties properties;
    private SearchKeywordSelector selector;
    private long nextId;

    @BeforeEach
    void setUp() {
        keywordRepository = Mockito.mock(SearchKeywordRepository.class);
        voteRepository = Mockito.mock(VoteRepository.class);
        properties = new SearchKeywordProperties();
        properties.setEnabled(true);
        Clock clock = Clock.fixed(INSTANT, ZONE);
        selector = new SearchKeywordSelector(keywordRepository, voteRepository, properties, clock);
        nextId = 1;
        when(voteRepository.findAllActiveTitles()).thenReturn(List.of());
    }

    private SearchKeyword kw(SearchKeywordSource source, String keyword, Integer volume, Integer impressions,
                             LocalDateTime lastUsedAt) {
        SearchKeyword k = SearchKeyword.create(source, keyword, KeywordNormalizer.normalize(keyword), NOW);
        k.refreshMetrics(volume, impressions, null, null, NOW);
        if (lastUsedAt != null) {
            k.markUsed(lastUsedAt);
        }
        ReflectionTestUtils.setField(k, "id", nextId++);
        return k;
    }

    private SearchKeyword naver(String keyword, Integer volume) {
        return kw(SearchKeywordSource.NAVER_SEARCH_AD, keyword, volume, null, null);
    }

    private SearchKeyword google(String keyword, Integer impressions) {
        return kw(SearchKeywordSource.GOOGLE_SEARCH_CONSOLE, keyword, null, impressions, null);
    }

    @Test
    void filters_brand_cooldown_length_and_existing_titles() {
        SearchKeyword recentlyUsed = kw(SearchKeywordSource.NAVER_SEARCH_AD, "최근사용", 100, null, NOW.minusDays(10));
        when(keywordRepository.findByLastSeenAtGreaterThanEqual(any())).thenReturn(List.of(
                naver("발란세", 100),
                recentlyUsed,
                naver("가", 100),
                naver("가".repeat(26), 100),
                naver("부먹찍먹", 100),
                naver("깻잎논쟁", 100)));
        when(keywordRepository.findByLastUsedAtGreaterThanEqual(any())).thenReturn(List.of(recentlyUsed));
        when(voteRepository.findAllActiveTitles()).thenReturn(List.of("탕수육 부먹 vs 찍먹"));

        List<KeywordCandidate> result = selector.selectForPrompt();

        assertThat(result).extracting(KeywordCandidate::keyword).containsExactly("깻잎논쟁");
    }

    @Test
    void merges_candidates_that_look_identical_after_sanitize() {
        // "LOL[프로]"와 "LOL 프로"는 원본 정규화는 다르지만([] 때문에), 프롬프트에 보일 때는
        // 둘 다 "LOL 프로"로 정제되어 모델 눈에는 똑같은 키워드로 보인다.
        SearchKeyword bracketForm = naver("LOL[프로]", 100);
        SearchKeyword plainForm = google("LOL 프로", 50);
        when(keywordRepository.findByLastSeenAtGreaterThanEqual(any())).thenReturn(List.of(bracketForm, plainForm));

        List<KeywordCandidate> result = selector.selectForPrompt();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).ids()).containsExactlyInAnyOrder(bracketForm.getId(), plainForm.getId());
    }

    @Test
    void excludes_whole_group_when_sibling_row_was_recently_used() {
        // 네이버의 "LOL 프로"는 10일 전 사용 기록이 있어 그 행만 보면 쿨다운에 걸린다. GSC가
        // 같은 검색어를 이번 주에 처음 수집하면 그 행은 lastUsedAt이 없어 행 단위로만 보면
        // 통과해버린다 - 하지만 같은 표현이 10일 전에 이미 쓰였으므로 묶음 전체가 제외돼야 한다.
        SearchKeyword naverUsedRecently = kw(SearchKeywordSource.NAVER_SEARCH_AD, "LOL 프로", 100, null,
                NOW.minusDays(10));
        SearchKeyword googleNeverUsed = google("LOL 프로", 50);
        when(keywordRepository.findByLastSeenAtGreaterThanEqual(any()))
                .thenReturn(List.of(naverUsedRecently, googleNeverUsed));
        when(keywordRepository.findByLastUsedAtGreaterThanEqual(any()))
                .thenReturn(List.of(naverUsedRecently));

        List<KeywordCandidate> result = selector.selectForPrompt();

        assertThat(result).isEmpty();
    }

    @Test
    void excludes_keyword_when_usage_history_fell_outside_freshness_window() {
        // 네이버의 "LOL 프로"는 20일 전에 사용됐지만, 그 뒤로 네이버에서 다시 수집되지 않아
        // lastSeenAt이 30일 전으로 밀려나 findByLastSeenAtGreaterThanEqual(14일 범위)에는
        // 더 이상 잡히지 않는다. 그래도 사용 이력은 56일 쿨다운 안에 있으므로, 이번 주 GSC가
        // 같은 검색어를 새로 수집해도(lastUsedAt=null) 제외돼야 한다.
        SearchKeyword staleButRecentlyUsed = kw(SearchKeywordSource.NAVER_SEARCH_AD, "LOL 프로", 100, null,
                NOW.minusDays(20));
        ReflectionTestUtils.setField(staleButRecentlyUsed, "lastSeenAt", NOW.minusDays(30));
        when(keywordRepository.findByLastUsedAtGreaterThanEqual(any()))
                .thenReturn(List.of(staleButRecentlyUsed));

        SearchKeyword googleNeverUsed = google("LOL 프로", 50);
        when(keywordRepository.findByLastSeenAtGreaterThanEqual(any())).thenReturn(List.of(googleNeverUsed));

        List<KeywordCandidate> result = selector.selectForPrompt();

        assertThat(result).isEmpty();
    }

    @Test
    void excludes_stale_last_seen() {
        when(keywordRepository.findByLastSeenAtGreaterThanEqual(NOW.minusDays(14))).thenReturn(List.of());

        assertThat(selector.selectForPrompt()).isEmpty();
        verify(keywordRepository).findByLastSeenAtGreaterThanEqual(NOW.minusDays(14));
    }

    @Test
    void orders_naver_then_google_with_limits() {
        properties.getPrompt().setNaverLimit(2);
        properties.getPrompt().setGoogleLimit(1);
        when(keywordRepository.findByLastSeenAtGreaterThanEqual(any())).thenReturn(List.of(
                naver("네이버열", 10),
                naver("네이버널", null),
                naver("네이버오십", 50),
                google("구글하나", 5),
                google("구글둘", 9)));

        List<KeywordCandidate> result = selector.selectForPrompt();

        assertThat(result).extracting(KeywordCandidate::keyword)
                .containsExactly("네이버오십", "네이버열", "구글둘");
    }

    @Test
    void merges_same_normalized_keyword_across_sources() {
        SearchKeyword naverRow = naver("레트로게임", 100);
        SearchKeyword googleRow = google("레트로 게임", 50);
        when(keywordRepository.findByLastSeenAtGreaterThanEqual(any())).thenReturn(List.of(naverRow, googleRow));

        List<KeywordCandidate> result = selector.selectForPrompt();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).ids()).containsExactlyInAnyOrder(naverRow.getId(), googleRow.getId());
        assertThat(result.get(0).monthlySearchVolume()).isEqualTo(100);
        assertThat(result.get(0).impressions()).isEqualTo(50);
    }

    @Test
    void disabled_returns_empty() {
        properties.setEnabled(false);

        assertThat(selector.selectForPrompt()).isEmpty();
        verifyNoInteractions(keywordRepository);
        verifyNoInteractions(voteRepository);
    }

    @Test
    void select_failure_returns_empty() {
        when(keywordRepository.findByLastSeenAtGreaterThanEqual(any())).thenThrow(new RuntimeException("db down"));

        assertThat(selector.selectForPrompt()).isEmpty();
    }

    @Test
    void markUsed_updates_last_used_at_with_clock() {
        SearchKeyword k = naver("깻잎논쟁", 10);
        when(keywordRepository.findById(1L)).thenReturn(Optional.of(k));

        selector.markUsed(1L);

        assertThat(k.getLastUsedAt()).isEqualTo(NOW);
    }

    @Test
    void markUsed_ignores_missing() {
        when(keywordRepository.findById(9L)).thenReturn(Optional.empty());

        selector.markUsed(9L);

        verify(keywordRepository, never()).save(any());
    }
}
