package com.valanse.valanse.dto.Vote;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendingVotesResponse {
    private int requestedDays;
    private LocalDateTime from;
    private LocalDateTime to;
    private TrendingScoreType scoreType;
    private boolean fallbackApplied;
    private List<TrendingVoteItemResponse> votes;
}
