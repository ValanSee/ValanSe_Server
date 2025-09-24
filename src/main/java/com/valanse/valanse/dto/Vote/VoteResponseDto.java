package com.valanse.valanse.dto.Vote;

import lombok.Getter;

import com.valanse.valanse.domain.Vote;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class VoteResponseDto {
    private Long voteId;
    private String title;
    private String category;
    private int totalVoteCount;
    private String createdAt;
    private List<String> options;

    public VoteResponseDto(Vote vote) {
        this.voteId = vote.getId();
        this.title = vote.getTitle();
        this.category = vote.getCategory().name(); // enum to string
        this.totalVoteCount = vote.getTotalVoteCount();
        this.createdAt = vote.getCreatedAt().toLocalDate().toString();
        this.options = vote.getVoteOptions()
                .stream()
                .map(option -> option.getContent())  //  VoteOption에서 content 추출
                .collect(Collectors.toList());
    }
}