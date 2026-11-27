package com.valanse.valanse.dto.DailyVote;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * 지난 오늘의 밸런스 목록과 다음 페이지 커서(날짜)를 담는 응답 DTO 코드입니다.
 */
@Getter
@AllArgsConstructor
public class DailyVoteListResponse {
    private List<DailyVoteSummary> items;
    private String nextCursor;
}
