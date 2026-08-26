package com.valanse.valanse.dto.Vote;

import lombok.AllArgsConstructor;
import lombok.Getter;

import com.valanse.valanse.domain.Vote;
import java.util.List;
import java.util.stream.Collectors;

@Getter
/**
 * VoteResponseDto API 응답 또는 계층 간 전달 값을 담는 DTO 코드입니다.
 */
public class VoteResponseDto {
    private Long voteId;
    private String title;
    private String content; // 투표 상세 내용
    private String category;
    private int totalVoteCount;
    private String createdAt;
    private String creatorTitle;
    private List<OptionDto> options;

    /**
     * VoteResponseDto 의존성을 주입하거나 객체를 초기화하는 생성자입니다.
     */
    public VoteResponseDto(Vote vote) {
        this(vote, null);
    }

    /**
     * VoteResponseDto 의존성을 주입하거나 객체를 초기화하는 생성자입니다.
     */
    public VoteResponseDto(Vote vote, String creatorTitle) {
        this.voteId = vote.getId();
        this.title = vote.getTitle();
        this.content = vote.getContent(); // content 필드 추가
        this.category = vote.getCategory().name(); // enum to string
        this.totalVoteCount = vote.getTotalVoteCount();
        this.createdAt = vote.getCreatedAt().toLocalDate().toString();
        this.creatorTitle = creatorTitle;
        this.options = vote.getVoteOptions()
                .stream()
                .map(option -> new OptionDto(option.getContent(), option.getImageUrl()))
                .collect(Collectors.toList());
    }

    @Getter
    @AllArgsConstructor
    public static class OptionDto {
        private String content;
        private String imageUrl;
    }
}
