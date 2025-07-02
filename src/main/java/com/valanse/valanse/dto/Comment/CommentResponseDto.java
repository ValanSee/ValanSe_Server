package com.valanse.valanse.dto.Comment;

import com.valanse.valanse.domain.Comment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CommentResponseDto {

    private Long id;
    private String content;
    private Long memberId;
    private String memberName;
    private boolean isReply;
    private LocalDateTime createdAt;

    public static CommentResponseDto fromEntity(Comment comment) {
        return CommentResponseDto.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .memberId(comment.getMember().getId())
                .memberName(comment.getMember().getName()) // 이름 대신 닉네임 쓰려면 수정 가능
                .isReply(comment.getParent() != null)
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
