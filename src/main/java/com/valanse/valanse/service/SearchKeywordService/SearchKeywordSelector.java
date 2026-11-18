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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

            Set<String> cooldownBlockedKeywords = cooldownBlockedKeywords(fresh, cooldownThreshold);
            List<SearchKeyword> eligible = fresh.stream()
                    .filter(k -> !cooldownBlockedKeywords.contains(effectiveKeyword(k.getKeyword())))
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
            return mergeSameNormalizedKeyword(result);
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

    // 모델에게 실제로 보이는(sanitize 후 normalize된) 키워드 표현. 행 단위 cooldown 검사와
    // 후보 합치기(mergeSameNormalizedKeyword)가 "같은 키워드"를 같은 기준으로 판단하도록
    // 한곳에서 정의한다.
    private static String effectiveKeyword(String keyword) {
        return KeywordNormalizer.normalize(KeywordNormalizer.sanitize(keyword));
    }

    // 같은 표현(effectiveKeyword)의 행 중 하나라도 최근 reuseCooldownDays 안에 쓰였으면, 그
    // 표현 전체를 차단 목록에 올린다. 행 단위로만 검사하면 네이버 쪽을 쓴 지 얼마 안 됐는데
    // GSC가 같은 검색어를 이번에 처음 수집한 경우(lastUsedAt=null) 그 행만 보면 통과해버려서
    // 56일 재사용 제한을 우회한다.
    private static Set<String> cooldownBlockedKeywords(List<SearchKeyword> fresh, LocalDateTime cooldownThreshold) {
        return fresh.stream()
                .collect(Collectors.groupingBy(k -> effectiveKeyword(k.getKeyword())))
                .entrySet().stream()
                .filter(entry -> entry.getValue().stream()
                        .anyMatch(k -> k.getLastUsedAt() != null && !k.getLastUsedAt().isBefore(cooldownThreshold)))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private static KeywordCandidate toCandidate(SearchKeyword k) {
        return new KeywordCandidate(List.of(k.getId()), k.getSource(), k.getKeyword(),
                k.getMonthlySearchVolume(), k.getImpressions());
    }

    // 네이버·GSC 양쪽에 같은 키워드가 있거나(출처가 달라도), "LOL[프로]"와 "LOL 프로"처럼 원본은
    // 다르지만 프롬프트 표시용 정제(KeywordNormalizer.sanitize) 후에는 모델 눈에 똑같이 보이는
    // 후보가 있으면 하나로 합친다. 합치지 않으면 모델이 한쪽 행의 id로만 markUsed를 호출해
    // 다른 쪽 행은 계속 null로 남고, 56일 재사용 제한을 우회해 다음 주에 다시 뽑힌다. 매칭 쪽
    // (ContentSeedOrchestrator.findMatchingKeywordIds)도 같은 sanitize+normalize 키를 쓴다.
    private static List<KeywordCandidate> mergeSameNormalizedKeyword(List<KeywordCandidate> candidates) {
        Map<String, KeywordCandidate> merged = new LinkedHashMap<>();
        for (KeywordCandidate candidate : candidates) {
            merged.merge(effectiveKeyword(candidate.keyword()), candidate, SearchKeywordSelector::combine);
        }
        return List.copyOf(merged.values());
    }

    private static KeywordCandidate combine(KeywordCandidate first, KeywordCandidate second) {
        List<Long> ids = new ArrayList<>(first.ids());
        ids.addAll(second.ids());
        Integer volume = first.monthlySearchVolume() != null ? first.monthlySearchVolume() : second.monthlySearchVolume();
        Integer impressions = first.impressions() != null ? first.impressions() : second.impressions();
        return new KeywordCandidate(ids, first.source(), first.keyword(), volume, impressions);
    }
}
