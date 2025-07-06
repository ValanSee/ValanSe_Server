package com.valanse.valanse.dto.Vote;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoteCancleResponseDto {
    private boolean isVoted; // 투표를 취소한 요청인지 투표를 추가한 요청인지 구분
    private Integer totalVoteCount; // POST 요청 후 업데이트 된 총 투표 수
    private Long voteOptionId; // POST 요청 후 업데이트 된 선택지의 id
    private Integer voteOptionCount; // POST 요청 후 업데이트 된 선택지 별 투표 수
}