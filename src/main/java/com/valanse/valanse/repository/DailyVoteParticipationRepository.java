package com.valanse.valanse.repository;

import com.valanse.valanse.domain.DailyVoteParticipation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 오늘의 밸런스 참여 기록(DailyVoteParticipation)을 조회하고 저장하는 Spring Data JPA 리포지토리 코드입니다.
 */
public interface DailyVoteParticipationRepository extends JpaRepository<DailyVoteParticipation, Long> {

    Optional<DailyVoteParticipation> findByVoteDateAndVoteIdAndMember_Id(LocalDate voteDate, Long voteId, Long memberId);

    boolean existsByVoteDateAndMember_IdAndParticipationPointGreaterThan(LocalDate voteDate, Long memberId, int point);

    List<DailyVoteParticipation> findByVoteDateAndVoteId(LocalDate voteDate, Long voteId);

    long countByVoteDateAndVoteIdAndCurrentOptionId(LocalDate voteDate, Long voteId, Long currentOptionId);
}
