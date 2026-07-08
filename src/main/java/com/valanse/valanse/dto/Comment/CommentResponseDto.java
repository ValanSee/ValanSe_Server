package com.valanse.valanse.dto.Comment;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import com.querydsl.core.annotations.QueryProjection;


@Getter
@Builder
/**
 * CommentResponseDto API 응답 또는 계층 간 전달 값을 담는 DTO 코드입니다.
 */
public class CommentResponseDto {
    private Long commentId;
    private Long voteId;
    private String nickname;
    private String title;
    private LocalDateTime commentCreatedAt;  // 추가
    private LocalDateTime voteCreatedAt;     // 추가
    private String content;
    private Integer likeCount;
    private Integer replyCount;
    private LocalDateTime deletedAt;
    private String voteOptionLabel;
    private Long daysAgo;
    private Long hoursAgo;
    private Boolean canDelete;

    /**
     * CommentResponseDto 의존성을 주입하거나 객체를 초기화하는 생성자입니다.
     */
    @QueryProjection
    public CommentResponseDto(Long commentId, Long voteId, String nickname, String title,
                              LocalDateTime commentCreatedAt, LocalDateTime voteCreatedAt,  // 추가
                              String content, Integer likeCount, Integer replyCount,
                              LocalDateTime deletedAt, String voteOptionLabel, Long daysAgo, Long hoursAgo,
                              Boolean canDelete) {
        this.commentId = commentId;
        this.voteId = voteId;
        this.nickname = nickname;
        this.title = title;
        this.commentCreatedAt = commentCreatedAt;
        this.voteCreatedAt = voteCreatedAt;
        this.content = content;
        this.likeCount = likeCount;
        this.replyCount = replyCount;
        this.deletedAt = deletedAt;
        this.voteOptionLabel = voteOptionLabel;
        this.daysAgo = daysAgo;
        this.hoursAgo = hoursAgo;
        this.canDelete = canDelete;
    }
}
