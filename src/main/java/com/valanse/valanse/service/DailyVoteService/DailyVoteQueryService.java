package com.valanse.valanse.service.DailyVoteService;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.common.message.DailyVoteErrorMessage;
import com.valanse.valanse.domain.DailyVote;
import com.valanse.valanse.domain.DailyVoteParticipation;
import com.valanse.valanse.domain.Vote;
import com.valanse.valanse.dto.DailyVote.DailyVoteListResponse;
import com.valanse.valanse.dto.DailyVote.DailyVoteSummary;
import com.valanse.valanse.dto.DailyVote.DailyVoteTally;
import com.valanse.valanse.dto.DailyVote.DailyVoteTodayResponse;
import com.valanse.valanse.repository.DailyVoteParticipationRepository;
import com.valanse.valanse.repository.DailyVoteRepository;
import com.valanse.valanse.repository.MemberBlockRepository;
import com.valanse.valanse.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

/**
 * 오늘의 밸런스 공개 조회(오늘 게임, 지난 날 목록/상세)를 담당하는 서비스 코드입니다. 비로그인(memberId null)도 조회할 수 있습니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyVoteQueryService {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 50;

    private final DailyVoteRepository dailyVoteRepository;
    private final DailyVoteParticipationRepository participationRepository;
    private final DailyVoteParticipationService participationService;
    private final MemberBlockRepository memberBlockRepository;
    private final VoteRepository voteRepository;
    private final Clock clock;

    /**
     * 오늘 행이 없으면 empty 를 반환합니다(컨트롤러에서 204).
     */
    public Optional<DailyVoteTodayResponse> getToday(Long memberId) {
        LocalDate today = LocalDate.now(clock);
        return dailyVoteRepository.findById(today).map(dv -> toTodayResponse(dv, today, memberId));
    }

    public DailyVoteListResponse getList(String cursor, Integer size, Long memberId) {
        int pageSize = size == null ? DEFAULT_SIZE : size;
        if (pageSize < 1 || pageSize > MAX_SIZE) {
            throw new ApiException(DailyVoteErrorMessage.DAILY_VOTE_SIZE_INVALID.message(), HttpStatus.BAD_REQUEST);
        }
        LocalDate today = LocalDate.now(clock);
        LocalDate before = today;
        if (cursor != null && !cursor.isBlank()) {
            LocalDate parsed = parseDate(cursor);
            before = parsed.isBefore(today) ? parsed : today;
        }
        Slice<DailyVote> slice = dailyVoteRepository
                .findByVoteDateLessThanAndSettledAtIsNotNullOrderByVoteDateDesc(before, PageRequest.of(0, pageSize));
        List<DailyVote> rows = slice.getContent();
        List<DailyVoteSummary> items = rows.stream().map(dv -> toSummary(dv, memberId)).toList();
        String nextCursor = slice.hasNext() && !rows.isEmpty()
                ? rows.get(rows.size() - 1).getVoteDate().toString()
                : null;
        return new DailyVoteListResponse(items, nextCursor);
    }

    public DailyVoteSummary getByDate(String dateText, Long memberId) {
        LocalDate date = parseDate(dateText);
        if (date.isAfter(LocalDate.now(clock))) {
            throw notFound();
        }
        DailyVote dv = dailyVoteRepository.findById(date).orElseThrow(DailyVoteQueryService::notFound);
        return toSummary(dv, memberId);
    }

    private DailyVoteTodayResponse toTodayResponse(DailyVote dv, LocalDate today, Long memberId) {
        DailyVoteTally tally = participationService.tally(dv);
        Long authorId = dv.getVoteId() == null ? null
                : voteRepository.findById(dv.getVoteId()).map(Vote::getMember).map(m -> m.getId()).orElse(null);
        Boolean blocked = memberId == null ? null
                : authorId != null && memberBlockRepository.existsByBlockerIdAndBlockedId(memberId, authorId);

        DailyVoteTodayResponse.MyParticipation my = findParticipation(dv, memberId)
                .map(p -> DailyVoteTodayResponse.MyParticipation.builder()
                        .firstOptionId(p.getFirstOptionId())
                        .currentOptionId(p.getCurrentOptionId())
                        .participationPoint(p.getParticipationPoint())
                        .build())
                .orElse(null);

        DailyVoteSummary yesterday = dailyVoteRepository.findById(today.minusDays(1))
                .map(y -> toSummary(y, memberId))
                .orElse(null);

        return DailyVoteTodayResponse.builder()
                .date(today.toString())
                .endsAt(today.plusDays(1).atStartOfDay(clock.getZone()).toOffsetDateTime())
                .voteId(dv.getVoteId())
                .question(dv.getQuestionSnapshot())
                .options(List.of(
                        DailyVoteTodayResponse.Option.builder().id(dv.getOptionAId()).label("A").content(dv.getOptionASnapshot()).build(),
                        DailyVoteTodayResponse.Option.builder().id(dv.getOptionBId()).label("B").content(dv.getOptionBSnapshot()).build()))
                .todayACount(tally.aCount())
                .todayBCount(tally.bCount())
                .authorId(authorId)
                .authorBlockedByMe(blocked)
                .my(my)
                .yesterday(yesterday)
                .build();
    }

    /**
     * 정산 스냅샷 기준 요약입니다. 정산 전이면 표 수와 승자는 null 입니다.
     */
    private DailyVoteSummary toSummary(DailyVote dv, Long memberId) {
        boolean settled = dv.getSettledAt() != null;
        Integer myBonus = findParticipation(dv, memberId).map(DailyVoteParticipation::getBonusPoint).orElse(null);
        return DailyVoteSummary.builder()
                .date(dv.getVoteDate().toString())
                .voteId(dv.getVoteId())
                .question(dv.getQuestionSnapshot())
                .optionA(dv.getOptionASnapshot())
                .optionB(dv.getOptionBSnapshot())
                .aCount(settled ? dv.getSettledACount() : null)
                .bCount(settled ? dv.getSettledBCount() : null)
                .winner(settled && dv.getWinner() != null ? dv.getWinner().name() : null)
                .myBonusPoint(myBonus)
                .build();
    }

    private Optional<DailyVoteParticipation> findParticipation(DailyVote dv, Long memberId) {
        if (memberId == null || dv.getVoteId() == null) {
            return Optional.empty();
        }
        return participationRepository.findByVoteDateAndVoteIdAndMember_Id(dv.getVoteDate(), dv.getVoteId(), memberId);
    }

    private static LocalDate parseDate(String text) {
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new ApiException(DailyVoteErrorMessage.DAILY_VOTE_DATE_INVALID.message(), HttpStatus.BAD_REQUEST);
        }
    }

    private static ApiException notFound() {
        return new ApiException(DailyVoteErrorMessage.DAILY_VOTE_NOT_FOUND.message(), HttpStatus.NOT_FOUND);
    }
}
