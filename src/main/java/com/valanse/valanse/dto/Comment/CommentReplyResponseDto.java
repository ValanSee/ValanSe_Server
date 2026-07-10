package com.valanse.valanse.dto.Comment;

import com.querydsl.core.annotations.QueryProjection;
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

    /**
     * CommentReplyResponseDto 의존성을 주입하거나 객체를 초기화하는 생성자입니다.
     */
    @QueryProjection
    public CommentReplyResponseDto(Long id, String nickname, String title, LocalDateTime createdAt,
                                   String content, Integer likeCount, Integer replyCount,
                                   Boolean isDeleted, LocalDateTime deletedAt, VoteLabel label,
                                   Long daysAgo, Long hoursAgo, Boolean canDelete) {
        this.id = id;
        this.nickname = nickname != null ? nickname : "탈퇴한 사용자";
        this.title = title;
        this.createdAt = createdAt;
        this.content = content;
        this.likeCount = likeCount != null ? likeCount : 0;
        this.replyCount = replyCount != null ? replyCount : 0;
        this.isDeleted = Boolean.TRUE.equals(isDeleted);
        this.deletedAt = deletedAt;
        this.label = label;
        this.daysAgo = daysAgo;
        this.hoursAgo = hoursAgo;
        this.canDelete = Boolean.TRUE.equals(canDelete);
    }
}
