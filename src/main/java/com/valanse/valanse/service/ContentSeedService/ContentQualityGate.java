package com.valanse.valanse.service.ContentSeedService;

import com.valanse.valanse.domain.enums.VoteCategory;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

// 구조화 출력 성공 여부와 별개로, 저장 전 반드시 통과해야 하는 서버 품질 게이트.
// "금지 주제" 중 정치·종교·혐오 등 의미 판단이 필요한 항목은 프롬프트 지시(item4)와
// 운영자 수동 검수에 맡기고, 이 클래스는 기계적으로 정확히 판별 가능한 개인정보(PII)
// 패턴만 차단한다.
@Component
public class ContentQualityGate {

    private static final int TITLE_MAX_LENGTH = 25;
    private static final int BODY_MAX_LENGTH = 100;
    private static final int COMMENT_MAX_LENGTH = 80;

    // 제목 정규화 시 공백과 함께 제거할 특수문자 범위(ASCII 구두점 + CJK 기호/구두점 블록).
    private static final Pattern NORMALIZE_STRIP_PATTERN =
            Pattern.compile("[\\s\\p{Punct}\\u3000-\\u303F]");

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("[\\w.+-]+@[\\w-]+\\.[\\w.-]+");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("01[016789][-.\\s]?\\d{3,4}[-.\\s]?\\d{4}");

    // 공백, 특수문자, 대소문자 차이를 정리해 완전 동일 제목 비교용 키를 만든다.
    public String normalizeTitle(String title) {
        if (title == null) {
            return "";
        }
        String normalized = Normalizer.normalize(title, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        return NORMALIZE_STRIP_PATTERN.matcher(normalized).replaceAll("");
    }

    public boolean isDuplicateTitle(String candidateTitle, Collection<String> existingTitles) {
        String normalizedCandidate = normalizeTitle(candidateTitle);
        return existingTitles.stream()
                .map(this::normalizeTitle)
                .anyMatch(normalizedCandidate::equals);
    }

    public QualityCheckResult validatePost(GeneratedPost post, Collection<String> recentTitles) {
        List<String> reasons = new ArrayList<>();

        String title = post.title() == null ? "" : post.title().trim();
        if (title.isEmpty() || title.length() > TITLE_MAX_LENGTH) {
            reasons.add("제목 길이 위반(1~" + TITLE_MAX_LENGTH + "자)");
        } else if (isDuplicateTitle(title, recentTitles)) {
            reasons.add("최근 활성 게시글과 중복된 제목");
        }

        if (post.body() == null || post.body().isBlank() || post.body().length() > BODY_MAX_LENGTH) {
            reasons.add("본문 길이 위반(1~" + BODY_MAX_LENGTH + "자)");
        }

        validateOptions(post.optionA(), post.optionB(), reasons);

        if (post.category() == null || post.category() == VoteCategory.ALL) {
            reasons.add("카테고리 위반(ALL 사용 불가)");
        }

        if (containsPersonalInformation(title) || containsPersonalInformation(post.body())
                || containsPersonalInformation(post.optionA()) || containsPersonalInformation(post.optionB())) {
            reasons.add("개인정보로 추정되는 문자열 포함");
        }

        return reasons.isEmpty() ? QualityCheckResult.pass() : QualityCheckResult.reject(reasons);
    }

    public QualityCheckResult validateInteraction(GeneratedInteraction interaction) {
        List<String> reasons = new ArrayList<>();

        if (interaction.targetPostId() == null) {
            reasons.add("대상 게시글 id 누락");
        }
        if (interaction.selectedOption() == null) {
            reasons.add("선택 옵션 누락");
        }

        String comment = interaction.comment();
        if (comment == null || comment.isBlank() || comment.length() > COMMENT_MAX_LENGTH) {
            reasons.add("댓글 길이 위반(1~" + COMMENT_MAX_LENGTH + "자)");
        } else if (containsPersonalInformation(comment)) {
            reasons.add("개인정보로 추정되는 문자열 포함");
        }

        return reasons.isEmpty() ? QualityCheckResult.pass() : QualityCheckResult.reject(reasons);
    }

    private void validateOptions(String optionA, String optionB, List<String> reasons) {
        boolean aBlank = optionA == null || optionA.isBlank();
        boolean bBlank = optionB == null || optionB.isBlank();
        if (aBlank || bBlank) {
            reasons.add("선택지에 공백 존재");
        } else if (optionA.trim().equals(optionB.trim())) {
            reasons.add("선택지 중복");
        }
    }

    // 이메일·전화번호처럼 보이는 문자열이 포함되어 있는지 검사한다.
    public boolean containsPersonalInformation(String text) {
        if (text == null) {
            return false;
        }
        return EMAIL_PATTERN.matcher(text).find() || PHONE_PATTERN.matcher(text).find();
    }

    // 이메일·전화번호처럼 보이는 문자열을 마스킹한다.
    public String maskPersonalInformation(String text) {
        if (text == null) {
            return null;
        }
        String masked = EMAIL_PATTERN.matcher(text).replaceAll("[masked-email]");
        return PHONE_PATTERN.matcher(masked).replaceAll("[masked-phone]");
    }

    // 로그·Discord 알림용 짧은 요약. 개인정보를 마스킹하고 길이를 제한해
    // 품질 미달 원문 전체가 그대로 로그에 남지 않도록 한다.
    public String toLogSafeSummary(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        String masked = maskPersonalInformation(text);
        return masked.length() <= maxLength ? masked : masked.substring(0, maxLength) + "...";
    }
}
