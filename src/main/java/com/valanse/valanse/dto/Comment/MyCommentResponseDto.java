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
    private String title; // 추가
    private String content;
    private Long memberId;
    private String memberName;
    private boolean isReply;
    private LocalDateTime createdAt;

    public static MyCommentResponseDto fromEntity(Comment comment) {
        return MyCommentResponseDto.builder()
                .id(comment.getId())
                .title(comment.getTitle())    //  추가
                .content(comment.getContent())
                .memberId(comment.getMember().getId())
                .memberName(comment.getMember().getName())
                .isReply(comment.getParent() != null)
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
