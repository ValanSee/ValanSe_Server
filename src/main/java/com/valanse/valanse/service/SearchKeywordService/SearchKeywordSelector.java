package com.valanse.valanse.service.SearchKeywordService;

import com.valanse.valanse.common.config.SearchKeywordProperties;
import com.valanse.valanse.domain.SearchKeyword;
import com.valanse.valanse.domain.enums.SearchKeywordSource;
import com.valanse.valanse.repository.SearchKeywordRepository;
import com.valanse.valanse.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// 주간 게시글 생성 프롬프트에 넣을 검색 키워드를 고르고 사용 기록을 남긴다.
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchKeywordSelector {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 25;

    private final SearchKeywordRepository keywordRepository;
    private final VoteRepository voteRepository;
    private final SearchKeywordProperties properties;
    private final Clock clock;

    public List<KeywordCandidate> selectForPrompt() {
        if (!properties.isEnabled()) {
            return List.of();
        }
        try {
            LocalDateTime now = LocalDateTime.now(clock);
            LocalDateTime cooldownThreshold = now.minusDays(properties.getReuseCooldownDays());
            List<SearchKeyword> fresh = keywordRepository
                    .findByLastSeenAtGreaterThanEqual(now.minusDays(properties.getFreshnessDays()));
            List<String> titles = voteRepository.findAllActiveTitles();

            List<SearchKeyword> eligible = fresh.stream()
                    .filter(k -> k.getLastUsedAt() == null || k.getLastUsedAt().isBefore(cooldownThreshold))
                    .filter(k -> isLengthOk(k.getKeyword()))
                    .filter(k -> !KeywordNormalizer.containsBrand(k.getKeyword(), properties.getBrandTerms()))
                    .filter(k -> titles.stream().noneMatch(title -> KeywordNormalizer.overlaps(k.getKeyword(), title)))
                    .toList();

            List<KeywordCandidate> result = new ArrayList<>();
            eligible.stream()
                    .filter(k -> k.getSource() == SearchKeywordSource.NAVER_SEARCH_AD)
                    .sorted(Comparator.comparing(SearchKeyword::getMonthlySearchVolume,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(properties.getPrompt().getNaverLimit())
                    .forEach(k -> result.add(toCandidate(k)));
            eligible.stream()
                    .filter(k -> k.getSource() == SearchKeywordSource.GOOGLE_SEARCH_CONSOLE)
                    .sorted(Comparator.comparing(SearchKeyword::getImpressions,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(properties.getPrompt().getGoogleLimit())
                    .forEach(k -> result.add(toCandidate(k)));
            return result;
        } catch (Exception e) {
            log.warn("검색 키워드 선택 실패, 키워드 없이 진행합니다.", e);
            return List.of();
        }
    }

    // 호출자(오케스트레이터)가 예외를 처리한다. 별도 트랜잭션이라 게시글 저장과 분리된다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markUsed(Long keywordId) {
        keywordRepository.findById(keywordId)
                .ifPresent(k -> k.markUsed(LocalDateTime.now(clock)));
    }

    private static boolean isLengthOk(String keyword) {
        int length = KeywordNormalizer.normalize(keyword).length();
        return length >= MIN_LENGTH && length <= MAX_LENGTH;
    }

    private static KeywordCandidate toCandidate(SearchKeyword k) {
        return new KeywordCandidate(k.getId(), k.getSource(), k.getKeyword(),
                k.getMonthlySearchVolume(), k.getImpressions());
    }
}
