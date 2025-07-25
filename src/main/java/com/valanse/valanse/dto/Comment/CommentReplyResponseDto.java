package com.valanse.valanse.dto.Comment;

import com.valanse.valanse.domain.enums.VoteLabel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CommentReplyResponseDto {

    private Long id;
    private String nickname;
    private LocalDateTime createdAt;
    private String content;
    private int likeCount;
    private int replyCount;
    private boolean isDeleted;
    private LocalDateTime deletedAt;
    private VoteLabel label;
    private Long daysAgo;
    private Long hoursAgo;
}