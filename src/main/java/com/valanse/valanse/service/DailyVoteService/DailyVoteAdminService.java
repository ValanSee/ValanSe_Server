package com.valanse.valanse.service.DailyVoteService;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.common.message.DailyVoteErrorMessage;
import com.valanse.valanse.domain.DailyVote;
import com.valanse.valanse.domain.Vote;
import com.valanse.valanse.domain.enums.DailyVoteSource;
import com.valanse.valanse.domain.enums.VoteLabel;
import com.valanse.valanse.dto.DailyVote.DailyVoteAdminItem;
import com.valanse.valanse.repository.DailyVoteRepository;
import com.valanse.valanse.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

/**
 * CMS 의 오늘의 밸런스 조회·예약·취소를 담당하는 관리자 서비스 코드입니다. 변경은 내일 이후 날짜만, 예약은 선택지 2개(A, B)인 게임만 가능합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyVoteAdminService {

    public static final int MAX_RANGE_DAYS = 62;

    private final DailyVoteRepository dailyVoteRepository;
    private final VoteRepository voteRepository;
    private final Clock clock;

    /**
     * from~to(양끝 포함) 범위의 행만 반환합니다. 빈 날짜는 포함하지 않습니다.
     */
    public List<DailyVoteAdminItem> list(String fromText, String toText) {
        LocalDate from = parseDate(fromText);
        LocalDate to = parseDate(toText);
        if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) + 1 > MAX_RANGE_DAYS) {
            throw bad(DailyVoteErrorMessage.DAILY_VOTE_RANGE_INVALID);
        }
        return dailyVoteRepository.findByVoteDateBetweenOrderByVoteDateAsc(from, to).stream()
                .map(DailyVoteAdminService::toItem)
                .toList();
    }

    /**
     * 내일 이후 날짜에 게임을 예약합니다. 이미 행이 있으면(자동 선정 포함) RESERVED 로 교체합니다.
     */
    @Transactional
    public DailyVoteAdminItem reserve(String dateText, Long voteId) {
        LocalDate date = requireEditableDate(dateText);
        Vote vote = voteId == null ? null : voteRepository.findById(voteId).orElse(null);
        if (vote == null) {
            throw new ApiException(DailyVoteErrorMessage.VOTE_NOT_FOUND.message(), HttpStatus.NOT_FOUND);
        }
        Set<VoteLabel> labels = vote.getVoteOptions().stream()
                .map(o -> o.getLabel()).collect(java.util.stream.Collectors.toSet());
        if (vote.getVoteOptions().size() != 2 || !labels.equals(Set.of(VoteLabel.A, VoteLabel.B))) {
            throw bad(DailyVoteErrorMessage.OPTION_COUNT_MUST_BE_TWO);
        }
        DailyVote dv = dailyVoteRepository.findById(date)
                .orElseGet(() -> DailyVote.builder().voteDate(date).build());
        dv.assign(vote, DailyVoteSource.RESERVED);
        return toItem(dailyVoteRepository.saveAndFlush(dv));
    }

    /**
     * 내일 이후 날짜의 행을 삭제합니다. 그날은 23:30 자동 선정이 다시 채웁니다.
     */
    @Transactional
    public void cancel(String dateText) {
        LocalDate date = requireEditableDate(dateText);
        DailyVote dv = dailyVoteRepository.findById(date)
                .orElseThrow(() -> new ApiException(DailyVoteErrorMessage.NOT_RESERVED.message(), HttpStatus.NOT_FOUND));
        dailyVoteRepository.delete(dv);
    }

    private LocalDate requireEditableDate(String dateText) {
        LocalDate date = parseDate(dateText);
        if (!date.isAfter(LocalDate.now(clock))) {
            throw bad(DailyVoteErrorMessage.PAST_OR_TODAY_NOT_EDITABLE);
        }
        return date;
    }

    private static DailyVoteAdminItem toItem(DailyVote dv) {
        boolean settled = dv.getSettledAt() != null;
        return DailyVoteAdminItem.builder()
                .date(dv.getVoteDate().toString())
                .voteId(dv.getVoteId())
                .question(dv.getQuestionSnapshot())
                .source(dv.getSource().name())
                .settled(settled)
                .aCount(settled ? dv.getSettledACount() : null)
                .bCount(settled ? dv.getSettledBCount() : null)
                .build();
    }

    private static LocalDate parseDate(String text) {
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException | NullPointerException e) {
            throw bad(DailyVoteErrorMessage.DAILY_VOTE_DATE_INVALID);
        }
    }

    private static ApiException bad(DailyVoteErrorMessage message) {
        return new ApiException(message.message(), HttpStatus.BAD_REQUEST);
    }
}
