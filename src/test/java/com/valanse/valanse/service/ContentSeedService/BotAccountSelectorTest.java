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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

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

    @Test
    void 봇_2개씩_1_2_3주차가_AB_CD_EA_순서로_순환한다() {
        when(memberRepository.findByIsBotTrueAndDeletedAtIsNullOrderByIdAsc()).thenReturn(fiveBots());
        properties.setBotsPerRun(2);

        List<String> week1 = socialIds(selector.selectActiveBots(REFERENCE_MONDAY));
        List<String> week2 = socialIds(selector.selectActiveBots(REFERENCE_MONDAY.plusWeeks(1)));
        List<String> week3 = socialIds(selector.selectActiveBots(REFERENCE_MONDAY.plusWeeks(2)));

        assertThat(week1).containsExactly("content-seed-bot-001", "content-seed-bot-002");
        assertThat(week2).containsExactly("content-seed-bot-003", "content-seed-bot-004");
        assertThat(week3).containsExactly("content-seed-bot-005", "content-seed-bot-001");
    }

    @Test
    void botsPerRun이_4일_때도_봇_5개_범위를_wrap_around_한다() {
        when(memberRepository.findByIsBotTrueAndDeletedAtIsNullOrderByIdAsc()).thenReturn(fiveBots());
        properties.setBotsPerRun(4);

        List<String> week1 = socialIds(selector.selectActiveBots(REFERENCE_MONDAY));
        List<String> week2 = socialIds(selector.selectActiveBots(REFERENCE_MONDAY.plusWeeks(1)));

        assertThat(week1).containsExactly(
                "content-seed-bot-001", "content-seed-bot-002", "content-seed-bot-003", "content-seed-bot-004");
        assertThat(week2).containsExactly(
                "content-seed-bot-005", "content-seed-bot-001", "content-seed-bot-002", "content-seed-bot-003");
    }

    @Test
    void botsPerRun이_5일_때는_매주_봇_5개_전부가_선택된다() {
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
    void 같은_주_안에서는_요일과_무관하게_같은_조합을_반환한다_자동_수동_동일_조합() {
        when(memberRepository.findByIsBotTrueAndDeletedAtIsNullOrderByIdAsc()).thenReturn(fiveBots());
        properties.setBotsPerRun(2);

        LocalDate monday = REFERENCE_MONDAY.plusWeeks(3);
        LocalDate sameSaturday = monday.plusDays(5);

        assertThat(selector.selectActiveBots(sameSaturday))
                .containsExactlyElementsOf(selector.selectActiveBots(monday));
    }

    @Test
    void 연말_연초_경계를_넘어가도_순환이_끊기지_않는다() {
        when(memberRepository.findByIsBotTrueAndDeletedAtIsNullOrderByIdAsc()).thenReturn(fiveBots());
        properties.setBotsPerRun(2);

        // REFERENCE_MONDAY(2026-01-05)로부터 51주 뒤 월요일은 2026-12-28, 그 다음 주는 2027-01-04.
        LocalDate lastMondayOf2026 = LocalDate.of(2026, 12, 28);
        LocalDate firstMondayOf2027 = LocalDate.of(2027, 1, 4);

        List<String> lastWeek2026 = socialIds(selector.selectActiveBots(lastMondayOf2026));
        List<String> firstWeek2027 = socialIds(selector.selectActiveBots(firstMondayOf2027));

        // 연속된 두 주이므로 다음 조합으로 정확히 한 칸 이어져야 한다(끊기거나 되돌아가지 않음).
        assertThat(firstWeek2027).isNotEqualTo(lastWeek2026);
        assertThat(socialIds(selector.selectActiveBots(firstMondayOf2027.minusWeeks(1))))
                .containsExactlyElementsOf(lastWeek2026);
    }

    @Test
    void 활성_봇이_없으면_빈_목록을_반환한다() {
        when(memberRepository.findByIsBotTrueAndDeletedAtIsNullOrderByIdAsc()).thenReturn(List.of());
        properties.setBotsPerRun(2);

        assertThat(selector.selectActiveBots(REFERENCE_MONDAY)).isEmpty();
    }
}
