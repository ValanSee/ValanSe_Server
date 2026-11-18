package com.valanse.valanse.service.SearchKeywordService;

import java.util.List;
import java.util.Locale;

/**
 * 검색 키워드 정규화·겹침 판정·브랜드 포함 여부 유틸.
 */
public final class KeywordNormalizer {

    private static final List<String> CORE_TOKENS = List.of("밸런스게임", "발란스게임", "밸런스", "게임", "vs");
    private static final int MIN_CORE_LENGTH = 3;

    private KeywordNormalizer() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    // 프롬프트에 보여줄 때 제어 문자·줄/문단 구분자·[]·백틱을 공백으로 바꾼다(ClaudeContentGenerator의
    // 표시용 정제와 동일). 모델은 이 정제된 형태만 보고 sourceKeyword에 그대로 echo하므로, 원본
    // keyword와 매칭할 때도 같은 정제를 거쳐야 괄호·백틱이 든 키워드가 영영 매칭 실패하지 않는다.
    public static String sanitize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replaceAll("[\\p{Cc}\\p{Zl}\\p{Zp}\\[\\]`]", " ")
                .replaceAll(" {2,}", " ")
                .trim();
    }

    public static String core(String raw) {
        String result = normalize(raw);
        for (String token : CORE_TOKENS) {
            result = result.replace(token, "");
        }
        return result;
    }

    public static boolean overlaps(String keyword, String title) {
        String keywordCore = core(keyword);
        if (keywordCore.length() < MIN_CORE_LENGTH) {
            return false;
        }
        String titleCore = core(title);
        if (titleCore.isEmpty()) {
            return false;
        }
        return titleCore.contains(keywordCore) || keywordCore.contains(titleCore);
    }

    public static boolean containsBrand(String keyword, List<String> brandTerms) {
        if (brandTerms == null) {
            return false;
        }
        String normalized = normalize(keyword);
        return brandTerms.stream()
                .map(KeywordNormalizer::normalize)
                .filter(term -> !term.isEmpty())
                .anyMatch(normalized::contains);
    }
}
