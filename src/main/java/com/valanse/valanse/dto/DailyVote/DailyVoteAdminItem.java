package com.valanse.valanse.dto.DailyVote;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * CMS 오늘의 게임 화면용 하루치 항목(게임, 출처, 정산 여부, 정산 표 수)을 담는 응답 DTO 코드입니다. 정산 전이면 aCount/bCount 는 null 입니다.
 */
@Getter
@Builder
public class DailyVoteAdminItem {
    private String date;
    private Long voteId;
    private String question;
    private String source;
    private boolean settled;
    @Getter(onMethod_ = @JsonProperty("aCount"))
    private Integer aCount;
    @Getter(onMethod_ = @JsonProperty("bCount"))
    private Integer bCount;
}
