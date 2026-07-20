package com.valanse.valanse.repository.VotesCheckRepositoryCustom;

import com.valanse.valanse.domain.Vote;
import com.valanse.valanse.dto.Vote.TrendingVoteScore;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * DB 조회와 저장을 담당하는 레포지토리 코드입니다.
 */
public interface VoteRepositoryCustom {
    List<Vote> findVotesByCursor(String category, String sort, String cursor, int size);
    List<TrendingVoteScore> findTopTrendingVotes(LocalDateTime from, LocalDateTime to, int limit);
    List<TrendingVoteScore> findTopAllTimeTrendingVotes(int limit);
    Optional<TrendingVoteScore> findTrendingScoreByVoteId(Long voteId, LocalDateTime from, LocalDateTime to);
    Optional<TrendingVoteScore> findAllTimeTrendingScoreByVoteId(Long voteId);
    List<Vote> findTrendingVoteDetailsByIds(List<Long> voteIds);
}
