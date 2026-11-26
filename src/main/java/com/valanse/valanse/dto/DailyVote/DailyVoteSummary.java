package com.valanse.valanse.dto.DailyVote;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * 하루치 오늘의 밸런스 요약(질문, 선택지, 정산 결과, 내 보너스)을 담는 응답 DTO 코드입니다. 정산 전이면 aCount/bCount/winner 는 null 입니다.
 */
@Getter
@Builder
public class DailyVoteSummary {
    private String date;
    private Long voteId;
    private String question;
    private String optionA;
    private String optionB;
    @Getter(onMethod_ = @JsonProperty("aCount"))
    private Integer aCount;
    @Getter(onMethod_ = @JsonProperty("bCount"))
    private Integer bCount;
    private String winner;
    private Integer myBonusPoint;
}
