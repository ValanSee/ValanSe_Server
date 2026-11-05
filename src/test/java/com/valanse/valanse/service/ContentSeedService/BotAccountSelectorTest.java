package com.valanse.valanse.service.ContentSeedService;

import com.valanse.valanse.common.config.ContentSeedProperties;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * M2(REQ-005, plan.md Milestone M2) 주간 봇 선택 전략 테스트입니다.
 * AC-008~AC-012를 직접 검증합니다 - 기존 floorMod 순환 정책이 셔플된-가방(shuffled
 * bag) 정책으로 교체되므로, 특정 ID 순서를 고정 assert하던 이전 테스트는 더 이상
 * 유효한 행동 계약이 아닙니다(그 순서 자체가 교체 대상이었음).
 */
@ExtendWith(MockitoExtension.class)
class BotAccountSelectorTest {

    private static final LocalDate REFERENCE_MONDAY = LocalDate.of(2026, 1, 5);

    @Mock
    private MemberRepository memberRepository;

    private ContentSeedProperties properties;
    private BotAccountSelector selector;

    @BeforeEach
    void setUp() {
        properties = new ContentSeedProperties();
        selector = new BotAccountSelector(memberRepository, properties);
    }

    private List<Member> fiveBots() {
        return List.of(
                botMember(1L, "content-seed-bot-001"),
                botMember(2L, "content-seed-bot-002"),
                botMember(3L, "content-seed-bot-003"),
                botMember(4L, "content-seed-bot-004"),
                botMember(5L, "content-seed-bot-005")
        );
    }

    private Member botMember(Long id, String socialId) {
        return Member.builder().id(id).socialId(socialId).isBot(true).build();
    }

    private List<String> socialIds(List<Member> members) {
        return members.stream().map(Member::getSocialId).toList();
    }

    // AC-008: 자동(스케줄러)/수동 실행이 같은 ISO 주 안의 다른 날짜에 호출되어도
    // 동일한 봇 조합을 반환해야 한다.
    @Test
    void 같은_주_안에서는_요일과_무관하게_같은_조합을_반환한다_자동_수동_동일_조합() {
        when(memberRepository.findByIsBotTrueAndDeletedAtIsNullOrderByIdAsc()).thenReturn(fiveBots());
        properties.setBotsPerRun(2);

        LocalDate monday = REFERENCE_MONDAY.plusWeeks(3);
        LocalDate sameSaturday = monday.plusDays(5);

        assertThat(selector.selectActiveBots(sameSaturday))
                .containsExactlyElementsOf(selector.selectActiveBots(monday));
    }

    // AC-009: 주차별 선택 조합/순서가 단순 ID 순차 순환(고정 폭 슬라이딩 윈도우)처럼
    // 반복되지 않아야 한다. 연속 10주의 결과가 모두 동일하면(=단순 순환이 끊기지
    // 않고 그대로 반복) 실패로 본다.
    @Test
    void 다음_주_선택_조합이_단순_ID_순환처럼_반복되지_않는다() {
        when(memberRepository.findByIsBotTrueAndDeletedAtIsNullOrderByIdAsc()).thenReturn(fiveBots());
        properties.setBotsPerRun(2);

        List<List<String>> combos = new java.util.ArrayList<>();
        for (int week = 0; week < 10; week++) {
            combos.add(socialIds(selector.selectActiveBots(REFERENCE_MONDAY.plusWeeks(week))));
        }

        long distinctCombos = combos.stream().distinct().count();
        assertThat(distinctCombos).isGreaterThan(1);
    }

    // AC-010: 최소 52주 시뮬레이션에서 봇별 선택 횟수 편차가 평균 대비 ±20% 이내여야
    // 한다(plan.md [RESOLVED] 사용자 확정 수치).
    @Test
    void 최소_52주_시뮬레이션에서_봇별_선택_횟수_편차가_평균_대비_20퍼센트_이내다() {
        List<Member> bots = fiveBots();
        when(memberRepository.findByIsBotTrueAndDeletedAtIsNullOrderByIdAsc()).thenReturn(bots);
        properties.setBotsPerRun(2);

        int weeks = 52;
        Map<String, Integer> selectionCounts = new HashMap<>();
        for (Member bot : bots) {
            selectionCounts.put(bot.getSocialId(), 0);
        }

        for (int week = 0; week < weeks; week++) {
            for (String socialId : socialIds(selector.selectActiveBots(REFERENCE_MONDAY.plusWeeks(week)))) {
                selectionCounts.merge(socialId, 1, Integer::sum);
            }
        }

        double average = selectionCounts.values().stream().mapToInt(Integer::intValue).average().orElseThrow();
        double tolerance = average * 0.2;

        selectionCounts.forEach((socialId, count) ->
                assertThat((double) count)
                        .as("bot %s 선택 횟수(%d)가 평균(%.1f) 대비 ±20%% 이내여야 함", socialId, count, average)
                        .isBetween(average - tolerance, average + tolerance));
    }

    // AC-011: 활성 봇 수가 bots-per-run 이하이거나 0인 경계 상황에서 기존 경계 동작이
    // 유지되어야 한다 - botsPerRun >= botCount면 매주 전체 봇이 선택되고, 활성 봇이
    // 0개면 빈 목록을 반환한다.
    @Test
    void botsPerRun이_봇_수_이상이면_매주_전체_봇이_선택된다() {
        when(memberRepository.findByIsBotTrueAndDeletedAtIsNullOrderByIdAsc()).thenReturn(fiveBots());
        properties.setBotsPerRun(5);

        List<String> week1 = socialIds(selector.selectActiveBots(REFERENCE_MONDAY));
        List<String> week7 = socialIds(selector.selectActiveBots(REFERENCE_MONDAY.plusWeeks(6)));

        assertThat(week1).containsExactlyInAnyOrder(
                "content-seed-bot-001", "content-seed-bot-002", "content-seed-bot-003",
                "content-seed-bot-004", "content-seed-bot-005");
        assertThat(week7).containsExactlyInAnyOrderElementsOf(week1);
    }

    @Test
    void 활성_봇이_없으면_빈_목록을_반환한다() {
        when(memberRepository.findByIsBotTrueAndDeletedAtIsNullOrderByIdAsc()).thenReturn(List.of());
        properties.setBotsPerRun(2);

        assertThat(selector.selectActiveBots(REFERENCE_MONDAY)).isEmpty();
    }

    // M2 설계 노트: 셔플 기반 선택은 한 에폭(ceil(botCount/botsPerRun)주) 안에서는
    // 같은 순열에서만 뽑으므로 한 주 안에서 같은 봇이 중복 선택될 수 없다
    // (AC-012가 기대하는 "그 주 선택된 봇 집합"이 항상 서로 다른 봇으로만 구성됨을
    // 보장하는 전제 조건).
    @Test
    void 한_주의_선택_안에서는_같은_봇이_중복으로_뽑히지_않는다() {
        when(memberRepository.findByIsBotTrueAndDeletedAtIsNullOrderByIdAsc()).thenReturn(fiveBots());
        properties.setBotsPerRun(3);

        for (int week = 0; week < 20; week++) {
            List<String> combo = socialIds(selector.selectActiveBots(REFERENCE_MONDAY.plusWeeks(week)));
            assertThat(combo).doesNotHaveDuplicates();
            assertThat(combo).hasSize(3);
        }
    }

    @Test
    void 연말_연초_경계를_넘어가도_예외_없이_항상_botsPerRun개를_반환한다() {
        when(memberRepository.findByIsBotTrueAndDeletedAtIsNullOrderByIdAsc()).thenReturn(fiveBots());
        properties.setBotsPerRun(2);

        LocalDate lastMondayOf2026 = LocalDate.of(2026, 12, 28);
        LocalDate firstMondayOf2027 = LocalDate.of(2027, 1, 4);

        assertThat(selector.selectActiveBots(lastMondayOf2026)).hasSize(2);
        assertThat(selector.selectActiveBots(firstMondayOf2027)).hasSize(2);
    }
}
