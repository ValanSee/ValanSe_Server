package com.valanse.valanse.dto.Comment;

import com.valanse.valanse.domain.enums.VoteLabel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
/**
 * CommentReplyResponseDto API 응답 또는 계층 간 전달 값을 담는 DTO 코드입니다.
 */
public class CommentReplyResponseDto {

    private Long id;
    private String nickname;
    private String title;
    private LocalDateTime createdAt;
    private String content;
    private int likeCount;
    private int replyCount;
    private boolean isDeleted;
    private LocalDateTime deletedAt;
    private VoteLabel label;
    private Long daysAgo;
    private Long hoursAgo;
    private boolean canDelete;
}
