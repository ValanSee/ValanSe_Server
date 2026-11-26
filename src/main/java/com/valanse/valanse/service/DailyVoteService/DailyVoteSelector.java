package com.valanse.valanse.service.DailyVoteService;

import com.valanse.valanse.domain.DailyVote;
import com.valanse.valanse.domain.Vote;
import com.valanse.valanse.domain.enums.DailyVoteSource;
import com.valanse.valanse.repository.DailyVoteRepository;
import com.valanse.valanse.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Component
@RequiredArgsConstructor
/**
 * 오늘의 밸런스 후보를 자동으로 고르고, 예약이 없는 날짜에 AUTO 로 채우는 선정 컴포넌트 코드입니다.
 */
public class DailyVoteSelector {

    private static final int RECENT_DAYS = 14;
    private static final Pageable FIRST = PageRequest.of(0, 1);

    private final VoteRepository voteRepository;
    private final DailyVoteRepository dailyVoteRepository;
    private final Clock clock;

    /**
     * 최근 14일(KST 기준) 안에 생성된 후보를 최신순으로 우선 고르고, 없으면 반응성 점수 순으로 고릅니다.
     */
    public Optional<Vote> pickCandidate() {
        LocalDateTime boundaryUtc = toUtc(LocalDate.now(clock).minusDays(RECENT_DAYS).atStartOfDay());
        return voteRepository.findDailyCandidatesRecent(boundaryUtc, FIRST).stream().findFirst()
                .or(() -> voteRepository.findDailyCandidatesByReactivity(FIRST).stream().findFirst());
    }

    /**
     * 해당 날짜의 오늘의 게임이 없으면 후보로 AUTO 생성합니다. 이미 있으면(예약 포함) 그대로 반환하고, 후보가 없으면 비어 있습니다.
     */
    @Transactional
    public Optional<DailyVote> ensureAutoFor(LocalDate date) {
        Optional<DailyVote> existing = dailyVoteRepository.findById(date);
        if (existing.isPresent()) {
            return existing;
        }
        return pickCandidate().map(vote -> {
            DailyVote dailyVote = DailyVote.builder().voteDate(date).build();
            dailyVote.assign(vote, DailyVoteSource.AUTO);
            return dailyVoteRepository.save(dailyVote);
        });
    }

    private LocalDateTime toUtc(LocalDateTime koreaWallClock) {
        return koreaWallClock.atZone(clock.getZone())
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();
    }
}
