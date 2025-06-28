package com.valanse.valanse.dto.Vote;

import com.valanse.valanse.domain.enums.VoteCategory;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class VoteDetailResponse {
    private Long voteId;
    private String title;
    private VoteCategory category;
    private Integer totalVoteCount;
    private String creatorNickname; // 생성자의 닉네임을 원한다고 가정
    private LocalDateTime createdAt;
    private List<VoteOptionDto> options; // 투표 옵션을 위한 DTO

    @Getter
    @Builder
    public static class VoteOptionDto {
        private Long optionId;
        private String content;
        private Integer voteCount;
        private String label;
    }
}