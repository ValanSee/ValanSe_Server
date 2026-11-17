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
