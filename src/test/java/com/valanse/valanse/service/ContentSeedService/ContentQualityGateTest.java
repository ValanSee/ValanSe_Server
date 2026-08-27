package com.valanse.valanse.service.ContentSeedService;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ContentQualityGateTest {

    private final ContentQualityGate gate = new ContentQualityGate();

    private GeneratedPost validPost() {
        return new GeneratedPost("치킨 vs 피자", "저녁 뭐 먹지 고민이야", "치킨", "피자", GeneratableVoteCategory.FOOD);
    }

    private GeneratedInteraction validInteraction() {
        return new GeneratedInteraction(1L, GeneratedInteraction.SelectedOption.A, "저는 치킨파라서 A 골랐어요");
    }

    @Test
    void 제목_정규화는_NFKC_소문자화_공백_특수문자_제거를_수행한다() {
        assertThat(gate.normalizeTitle("  치킨 VS 피자!! "))
                .isEqualTo(gate.normalizeTitle("치킨vs피자"));
    }

    @Test
    void 공백과_특수문자_대소문자만_다른_제목은_중복으로_판정한다() {
        boolean duplicate = gate.isDuplicateTitle("치킨 VS 피자!!", List.of("치킨vs피자", "다른 제목"));
        assertThat(duplicate).isTrue();
    }

    @Test
    void 완전히_다른_제목은_중복이_아니다() {
        boolean duplicate = gate.isDuplicateTitle("완전 새로운 제목", List.of("치킨vs피자"));
        assertThat(duplicate).isFalse();
    }

    @Test
    void 정상_게시글은_품질_게이트를_통과한다() {
        QualityCheckResult result = gate.validatePost(validPost(), List.of("다른 제목"));

        assertThat(result.passed()).isTrue();
        assertThat(result.reasons()).isEmpty();
    }

    @Test
    void 제목이_25자를_초과하면_거절된다() {
        String longTitle = "가".repeat(26);
        GeneratedPost post = new GeneratedPost(longTitle, "본문", "A", "B", GeneratableVoteCategory.FOOD);

        QualityCheckResult result = gate.validatePost(post, List.of());

        assertThat(result.passed()).isFalse();
        assertThat(result.reasons()).anyMatch(r -> r.contains("제목 길이"));
    }

    @Test
    void 최근_제목과_중복되면_거절된다() {
        QualityCheckResult result = gate.validatePost(validPost(), List.of("치킨VS피자"));

        assertThat(result.passed()).isFalse();
        assertThat(result.reasons()).anyMatch(r -> r.contains("중복된 제목"));
    }

    @Test
    void 본문이_100자를_초과하면_거절된다() {
        GeneratedPost post = new GeneratedPost("제목", "가".repeat(101), "A", "B", GeneratableVoteCategory.FOOD);

        QualityCheckResult result = gate.validatePost(post, List.of());

        assertThat(result.passed()).isFalse();
        assertThat(result.reasons()).anyMatch(r -> r.contains("본문 길이"));
    }

    @Test
    void 선택지가_공백이거나_서로_중복되면_거절된다() {
        GeneratedPost blankOption = new GeneratedPost("제목", "본문", " ", "피자", GeneratableVoteCategory.FOOD);
        GeneratedPost duplicateOption = new GeneratedPost("제목", "본문", "치킨", "치킨", GeneratableVoteCategory.FOOD);

        assertThat(gate.validatePost(blankOption, List.of()).reasons()).anyMatch(r -> r.contains("공백"));
        assertThat(gate.validatePost(duplicateOption, List.of()).reasons()).anyMatch(r -> r.contains("중복"));
    }

    @Test
    void 카테고리가_없으면_거절된다() {
        GeneratedPost post = new GeneratedPost("제목", "본문", "A", "B", null);

        QualityCheckResult result = gate.validatePost(post, List.of());

        assertThat(result.passed()).isFalse();
        assertThat(result.reasons()).anyMatch(r -> r.contains("카테고리"));
    }

    @Test
    void 본문에_이메일이나_전화번호가_있으면_거절된다() {
        GeneratedPost withEmail = new GeneratedPost("제목", "연락은 test@example.com 으로", "A", "B", GeneratableVoteCategory.FOOD);
        GeneratedPost withPhone = new GeneratedPost("제목", "010-1234-5678로 연락줘", "A", "B", GeneratableVoteCategory.FOOD);

        assertThat(gate.validatePost(withEmail, List.of()).reasons()).anyMatch(r -> r.contains("개인정보"));
        assertThat(gate.validatePost(withPhone, List.of()).reasons()).anyMatch(r -> r.contains("개인정보"));
    }

    @Test
    void 정상_상호작용은_품질_게이트를_통과한다() {
        QualityCheckResult result = gate.validateInteraction(validInteraction());

        assertThat(result.passed()).isTrue();
    }

    @Test
    void 대상_게시글_id나_선택_옵션이_없으면_거절된다() {
        GeneratedInteraction noTarget = new GeneratedInteraction(null, GeneratedInteraction.SelectedOption.A, "댓글");
        GeneratedInteraction noOption = new GeneratedInteraction(1L, null, "댓글");

        assertThat(gate.validateInteraction(noTarget).reasons()).anyMatch(r -> r.contains("대상 게시글"));
        assertThat(gate.validateInteraction(noOption).reasons()).anyMatch(r -> r.contains("선택 옵션"));
    }

    @Test
    void 댓글이_80자를_초과하면_거절된다() {
        GeneratedInteraction interaction = new GeneratedInteraction(1L, GeneratedInteraction.SelectedOption.A, "가".repeat(81));

        QualityCheckResult result = gate.validateInteraction(interaction);

        assertThat(result.passed()).isFalse();
        assertThat(result.reasons()).anyMatch(r -> r.contains("댓글 길이"));
    }

    @Test
    void 이메일과_전화번호를_감지한다() {
        assertThat(gate.containsPersonalInformation("test@example.com 입니다")).isTrue();
        assertThat(gate.containsPersonalInformation("010-1234-5678 입니다")).isTrue();
        assertThat(gate.containsPersonalInformation("평범한 댓글입니다")).isFalse();
    }

    @Test
    void 일반전화와_국제표기_전화번호도_감지한다() {
        assertThat(gate.containsPersonalInformation("02-1234-5678로 연락주세요")).isTrue();
        assertThat(gate.containsPersonalInformation("031-123-4567로 연락주세요")).isTrue();
        assertThat(gate.containsPersonalInformation("+82-10-1234-5678로 연락주세요")).isTrue();
    }

    @Test
    void 이메일과_전화번호를_마스킹한다() {
        String masked = gate.maskPersonalInformation("연락은 test@example.com 또는 010-1234-5678");

        assertThat(masked)
                .doesNotContain("test@example.com")
                .doesNotContain("010-1234-5678")
                .contains("[masked-email]")
                .contains("[masked-phone]");
    }

    @Test
    void 로그용_요약은_개인정보를_마스킹하고_길이를_제한한다() {
        String summary = gate.toLogSafeSummary("연락처는 test@example.com 입니다. " + "가".repeat(50), 20);

        assertThat(summary).doesNotContain("test@example.com");
        assertThat(summary.length()).isLessThanOrEqualTo(23); // maxLength + "..."
    }
}
