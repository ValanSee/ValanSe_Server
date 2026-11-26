package com.valanse.valanse.repository;

import com.valanse.valanse.domain.DailyVote;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 오늘의 밸런스(DailyVote) 데이터를 조회하고 저장하는 Spring Data JPA 리포지토리 코드입니다.
 */
public interface DailyVoteRepository extends JpaRepository<DailyVote, LocalDate> {

    Optional<DailyVote> findByVoteDateAndVoteId(LocalDate voteDate, Long voteId);

    List<DailyVote> findByVoteDateBetweenOrderByVoteDateAsc(LocalDate from, LocalDate to);

    Slice<DailyVote> findByVoteDateLessThanEqualOrderByVoteDateDesc(LocalDate date, Pageable pageable);

    Slice<DailyVote> findByVoteDateLessThanAndSettledAtIsNotNullOrderByVoteDateDesc(LocalDate date, Pageable pageable);

    List<DailyVote> findByVoteIdAndVoteDateGreaterThanEqual(Long voteId, LocalDate from);
}
