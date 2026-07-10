package com.valanse.valanse.dto.Vote;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
/**
 * 투표 목록 페이지 응답 값을 담는 DTO 코드입니다.
 */
public class PagedVoteResponse {
    private List<VoteResponseDto> votes;
    private int page;
    private int size;
    private boolean hasNext;
}
