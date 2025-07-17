package com.valanse.valanse.repository.VotesCheckRepositoryCustom;

import com.valanse.valanse.domain.Vote;

import java.util.List;

public interface VoteRepositoryCustom {
    List<Vote> findVotesByCursor(String category, String sort, String cursor, int size);
}