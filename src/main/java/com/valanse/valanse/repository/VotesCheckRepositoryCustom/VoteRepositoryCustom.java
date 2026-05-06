package com.valanse.valanse.repository.VotesCheckRepositoryCustom;

import com.valanse.valanse.domain.Vote;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VoteRepositoryCustom {
    List<Vote> findVotesByCursor(String category, String sort, String cursor, int size);
    Optional<Vote> findHotIssueVote();
    Optional<Vote> findTrendingVote(LocalDateTime from, LocalDateTime to);
}
