package com.valanse.valanse.dto.Comment;
// com.valanse.valanse.dto.Comment.MyCommentResponseDto.java

import lombok.Builder;
import lombok.Getter;
import com.valanse.valanse.domain.Comment;
import com.querydsl.core.annotations.QueryProjection;

import java.time.LocalDateTime;

@Getter
@Builder
public class MyCommentResponseDto {

    private Long id;
    private String title;
    private String content;
    private Long memberId;
    private String memberName;
    private boolean isReply;
    private LocalDateTime createdAt;
    private Long voteOwnerId;
    private String voteOwnerName;     // 추가
    private String voteTitle;         // 추가
    private String voteOptionLabel;   // 추가

    public static MyCommentResponseDto fromEntity(Comment comment) {
        return MyCommentResponseDto.builder()
                .id(comment.getId())
                .title(comment.getTitle())
                .content(comment.getContent())
                .memberId(comment.getMember().getId())
                .memberName(comment.getMember().getName())
                .isReply(comment.getParent() != null)
                .createdAt(comment.getCreatedAt())
                .voteOwnerId(comment.getCommentGroup().getVote().getMember().getId())
                .voteOwnerName(comment.getCommentGroup().getVote().getMember().getName())   // 추가
                .voteTitle(comment.getCommentGroup().getVote().getTitle())                  // 추가
                .voteOptionLabel(comment.getCommentGroup().getVote().getVoteOptions().stream()
                        .filter(vo -> vo.getId().equals(comment.getId()))                  //  comment에 연결된 voteOption id 매칭 필요
                        .map(vo -> vo.getLabel().name())
                        .findFirst()
                        .orElse(null))                                                     // 추가
                .build();
    }
}
