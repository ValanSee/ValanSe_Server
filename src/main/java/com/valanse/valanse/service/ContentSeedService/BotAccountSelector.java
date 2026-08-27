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
import java.util.List;

@Component
@RequiredArgsConstructor
public class BotAccountSelector {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    // 봇 순환의 기준 월요일. round-robin 오프셋은 이 날짜로부터 지난 주 수로 계산한다.
    private static final LocalDate REFERENCE_MONDAY = LocalDate.of(2026, 1, 5);

    private final MemberRepository memberRepository;
    private final ContentSeedProperties properties;

    public List<Member> selectActiveBots() {
        return selectActiveBots(LocalDate.now(SEOUL));
    }

    // referenceDate가 속한 주(월요일 기준)의 봇 조합을 반환한다. 자동 실행과
    // 같은 주의 수동 실행이 항상 같은 결과를 받도록, 요일과 무관하게 그 주의
    // 월요일로 정규화한 뒤 계산한다.
    public List<Member> selectActiveBots(LocalDate referenceDate) {
        List<Member> bots = memberRepository.findByIsBotTrueOrderByIdAsc();
        int botCount = bots.size();
        if (botCount == 0) {
            return List.of();
        }

        int botsPerRun = Math.min(properties.getBotsPerRun(), botCount);
        long weekOffset = weekOffset(referenceDate);
        int startIndex = (int) Math.floorMod(weekOffset * botsPerRun, botCount);

        List<Member> selected = new ArrayList<>(botsPerRun);
        for (int i = 0; i < botsPerRun; i++) {
            selected.add(bots.get((startIndex + i) % botCount));
        }
        return selected;
    }

    private long weekOffset(LocalDate referenceDate) {
        LocalDate mondayOfWeek = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return ChronoUnit.WEEKS.between(REFERENCE_MONDAY, mondayOfWeek);
    }
}
