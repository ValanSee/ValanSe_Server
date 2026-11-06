package com.valanse.valanse.service.ContentSeedService;

import com.valanse.valanse.common.config.ContentSeedProperties;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class BotAccountSelector {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    // 주차 계산의 기준 월요일. weekOffset은 이 날짜로부터 지난 주 수로 계산한다.
    private static final LocalDate REFERENCE_MONDAY = LocalDate.of(2026, 1, 5);

    // M2(REQ-005/AC-008~012): 셔플 시드를 주차 인덱스에서 바로 유도하지 않고 한 번
    // 더 섞어, 시드가 1,2,3...처럼 단순 연속값이어도 그 섞임이 예측 가능한 패턴으로
    // 보이지 않게 한다(피보나치 해싱 상수). 값 자체에 특별한 의미는 없다.
    private static final long SHUFFLE_MIX_CONSTANT = 0x9E3779B97F4A7C15L;

    private final MemberRepository memberRepository;
    private final ContentSeedProperties properties;

    public List<Member> selectActiveBots() {
        return selectActiveBots(LocalDate.now(SEOUL));
    }

    // referenceDate가 속한 주(월요일 기준)의 봇 조합을 반환한다. 자동 실행과
    // 같은 주의 수동 실행이 항상 같은 결과를 받도록, 요일과 무관하게 그 주의
    // 월요일로 정규화한 뒤 계산한다.
    //
    // M2 선택 전략(REQ-005, plan.md [RESOLVED] 채택안 ①): "주차별로 전체 봇을
    // 결정적으로 섞은 뒤 그 섞인 순서에서 봇을 뽑는" 방식이다. botCount를
    // botsPerRun개씩 소진하는 주기를 "에폭"이라 부르며, 한 에폭에 필요한 주차 수는
    // ceil(botCount / botsPerRun)이다. 에폭마다 시드가 다른 새 순열로 다시 섞으므로
    // 단순 ID 순환(기존 floorMod 정책)처럼 보이지 않으면서도(AC-009), 한 에폭 안에서는
    // 모든 봇이 (거의) 고르게 소진되어 장기 편향이 ±20% 이내로 유지된다(AC-010).
    // 한 주의 선택은 항상 같은 에폭의 같은 순열에서만 뽑으므로(에폭을 넘어 다음 에폭의
    // 순열과 섞이지 않음) 한 주 안에서 같은 봇이 중복 선택되는 일은 없다.
    public List<Member> selectActiveBots(LocalDate referenceDate) {
        List<Member> bots = memberRepository.findByIsBotTrueAndDeletedAtIsNullOrderByIdAsc();
        int botCount = bots.size();
        if (botCount == 0) {
            return List.of();
        }

        int botsPerRun = Math.min(properties.getBotsPerRun(), botCount);
        if (botsPerRun <= 0) {
            return List.of();
        }

        int weeksPerEpoch = (botCount + botsPerRun - 1) / botsPerRun; // ceil(botCount / botsPerRun)
        long weekIndex = weekOffset(referenceDate);
        long epoch = Math.floorDiv(weekIndex, weeksPerEpoch);
        int weekInEpoch = (int) Math.floorMod(weekIndex, weeksPerEpoch);

        List<Member> permutation = shuffledPermutation(bots, epoch);
        int base = weekInEpoch * botsPerRun;

        List<Member> selected = new ArrayList<>(botsPerRun);
        for (int i = 0; i < botsPerRun; i++) {
            selected.add(permutation.get((base + i) % botCount));
        }
        return selected;
    }

    private List<Member> shuffledPermutation(List<Member> bots, long epoch) {
        List<Member> copy = new ArrayList<>(bots);
        long seed = epoch * SHUFFLE_MIX_CONSTANT;
        Collections.shuffle(copy, new Random(seed));
        return copy;
    }

    private long weekOffset(LocalDate referenceDate) {
        LocalDate mondayOfWeek = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return ChronoUnit.WEEKS.between(REFERENCE_MONDAY, mondayOfWeek);
    }
}
