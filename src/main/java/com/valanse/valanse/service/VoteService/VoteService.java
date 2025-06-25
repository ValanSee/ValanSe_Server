// src/main/java/com/valanse/valanse/service/VoteService/VoteService.java
package com.valanse.valanse.service.VoteService;

import com.valanse.valanse.dto.Vote.HotIssueVoteResponse;

public interface VoteService {
    HotIssueVoteResponse getHotIssueVote();
}