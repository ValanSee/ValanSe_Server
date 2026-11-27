package com.valanse.valanse.service.DailyVoteService;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.common.message.VoteErrorMessage;
import com.valanse.valanse.domain.DailyVote;
import com.valanse.valanse.domain.DailyVoteParticipation;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.enums.PointType;
import com.valanse.valanse.dto.DailyVote.DailyVoteReward;
import com.valanse.valanse.dto.DailyVote.DailyVoteTally;
import com.valanse.valanse.repository.DailyVoteParticipationRepository;
import com.valanse.valanse.repository.DailyVoteRepository;
import com.valanse.valanse.service.PointService.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
/**
 * 오늘의 밸런스 참여 기록 저장, 날짜당 1회 참여 포인트 지급, 오늘 표 집계를 처리하는 서비스 코드입니다.
 */
public class DailyVoteParticipationServiceImpl implements DailyVoteParticipationService {

    private static final int PARTICIPATION_POINT = 3;

    private final DailyVoteRepository dailyVoteRepository;
    private final DailyVoteParticipationRepository participationRepository;
    private final PointService pointService;
    private final DailyVotePointEligibility pointEligibility;
    private final Clock clock;

    @Override
    public Optional<DailyVote> findTodayFor(Long voteId) {
        return dailyVoteRepository.findByVoteDateAndVoteId(LocalDate.now(clock), voteId);
    }

    @Override
    public boolean hasParticipated(DailyVote today, Long memberId) {
        return participationRepository
                .findByVoteDateAndVoteIdAndMember_Id(today.getVoteDate(), today.getVoteId(), memberId)
                .isPresent();
    }

    @Override
    @Transactional
    public DailyVoteReward participate(DailyVote today, Member member, Long chosenOptionId) {
        boolean alreadyRewarded = participationRepository
                .existsByVoteDateAndMember_IdAndParticipationPointGreaterThan(today.getVoteDate(), member.getId(), 0);
        // 봇·탈퇴·프로필 없는 회원은 givePoint 가 지급하지 않거나 예외를 내므로 지급·기록 모두 0으로 남긴다
        boolean give = !alreadyRewarded && pointEligibility.canReceive(member);
        int point = give ? PARTICIPATION_POINT : 0;

        // 동시 더블탭으로 유니크 위반이 나면 포인트 지급 전에 여기서 걸러낸다 (flush 후에 지급)
        try {
            participationRepository.saveAndFlush(DailyVoteParticipation.builder()
                    .voteDate(today.getVoteDate())
                    .voteId(today.getVoteId())
                    .member(member)
                    .firstOptionId(chosenOptionId)
                    .currentOptionId(chosenOptionId)
                    .participationPoint(point)
                    .build());
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(VoteErrorMessage.VOTE_ALREADY_PROCESSED.message(), HttpStatus.BAD_REQUEST);
        }
        if (give) {
            pointService.givePoint(member.getId(), PointType.DAILY_VOTE);
        }
        return new DailyVoteReward(point, alreadyRewarded);
    }

    @Override
    @Transactional
    public void updateCurrentOption(DailyVote today, Long memberId, Long optionIdOrNull) {
        participationRepository
                .findByVoteDateAndVoteIdAndMember_Id(today.getVoteDate(), today.getVoteId(), memberId)
                .ifPresent(p -> p.changeCurrentOption(optionIdOrNull));
    }

    @Override
    public DailyVoteTally tally(DailyVote dv) {
        long a = participationRepository.countByVoteDateAndVoteIdAndCurrentOptionId(
                dv.getVoteDate(), dv.getVoteId(), dv.getOptionAId());
        long b = participationRepository.countByVoteDateAndVoteIdAndCurrentOptionId(
                dv.getVoteDate(), dv.getVoteId(), dv.getOptionBId());
        return new DailyVoteTally(a, b);
    }
}
