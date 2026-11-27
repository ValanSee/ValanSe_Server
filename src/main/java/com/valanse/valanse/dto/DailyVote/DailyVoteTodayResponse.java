package com.valanse.valanse.dto.DailyVote;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 오늘의 밸런스 조회 응답 DTO 코드입니다. 질문/선택지는 daily_vote 스냅샷 기준입니다.
 */
@Getter
@Builder
public class DailyVoteTodayResponse {
    private String date;
    private OffsetDateTime endsAt;
    private Long voteId;
    private String question;
    private List<Option> options;
    private long todayACount;
    private long todayBCount;
    private Long authorId;
    private Boolean authorBlockedByMe;
    private MyParticipation my;
    private DailyVoteSummary yesterday;

    @Getter
    @Builder
    public static class Option {
        private Long id;
        private String label;
        private String content;
    }

    @Getter
    @Builder
    public static class MyParticipation {
        private Long firstOptionId;
        private Long currentOptionId;
        private int participationPoint;
    }
}
