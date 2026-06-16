package com.valanse.valanse.repository.VotesCheckRepositoryCustom;

import com.valanse.valanse.domain.Vote;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * DB 조회와 저장을 담당하는 레포지토리 코드입니다.
 */
public interface VoteRepositoryCustom {
    List<Vote> findVotesByCursor(String category, String sort, String cursor, int size);
    Optional<Vote> findHotIssueVote();
    Optional<Vote> findTrendingVote(LocalDateTime from, LocalDateTime to);
}
