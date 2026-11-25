package com.valanse.valanse.dto.Comment;

import com.valanse.valanse.domain.Member;
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
    // 댓글 작성자 회원 id (차단하기용). 탈퇴 후 퍼지된 작성자면 null
    private Long authorId;
    private String nickname;
    private Boolean isBot;
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
    // 현재 요청자(로그인 사용자)가 이 댓글에 좋아요를 눌렀는지 여부. 비로그인이면 항상 false
    private boolean liked;

    /**
     * QueryDSL 조회용 생성자입니다. (작성자 id 포함)
     */
    @QueryProjection
    public CommentResponseDto(Long commentId, Long voteId, Long authorId, String nickname, Boolean isBot, String title,
                              LocalDateTime commentCreatedAt, LocalDateTime voteCreatedAt,
                              String content, Integer likeCount, Integer replyCount,
                              LocalDateTime deletedAt, String voteOptionLabel, Long daysAgo, Long hoursAgo,
                              Boolean canDelete) {
        this(commentId, voteId, authorId, nickname, isBot, title, commentCreatedAt, voteCreatedAt, content, likeCount,
                replyCount, deletedAt, voteOptionLabel, daysAgo, hoursAgo, canDelete, false);
    }

    /**
     * 작성자 id 없이 생성하는 기존 호환용 생성자입니다.
     */
    public CommentResponseDto(Long commentId, Long voteId, String nickname, Boolean isBot, String title,
                              LocalDateTime commentCreatedAt, LocalDateTime voteCreatedAt,
                              String content, Integer likeCount, Integer replyCount,
                              LocalDateTime deletedAt, String voteOptionLabel, Long daysAgo, Long hoursAgo,
                              Boolean canDelete) {
        this(commentId, voteId, null, nickname, isBot, title, commentCreatedAt, voteCreatedAt, content, likeCount,
                replyCount, deletedAt, voteOptionLabel, daysAgo, hoursAgo, canDelete, false);
    }

    /**
     * 작성자 id 없이 liked 값을 포함해 생성하는 기존 호환용 생성자입니다.
     */
    public CommentResponseDto(Long commentId, Long voteId, String nickname, Boolean isBot, String title,
                              LocalDateTime commentCreatedAt, LocalDateTime voteCreatedAt,
                              String content, Integer likeCount, Integer replyCount,
                              LocalDateTime deletedAt, String voteOptionLabel, Long daysAgo, Long hoursAgo,
                              Boolean canDelete, boolean liked) {
        this(commentId, voteId, null, nickname, isBot, title, commentCreatedAt, voteCreatedAt, content, likeCount,
                replyCount, deletedAt, voteOptionLabel, daysAgo, hoursAgo, canDelete, liked);
    }

    /**
     * 전체 필드 생성자입니다. (@Builder 용)
     */
    public CommentResponseDto(Long commentId, Long voteId, Long authorId, String nickname, Boolean isBot, String title,
                              LocalDateTime commentCreatedAt, LocalDateTime voteCreatedAt,
                              String content, Integer likeCount, Integer replyCount,
                              LocalDateTime deletedAt, String voteOptionLabel, Long daysAgo, Long hoursAgo,
                              Boolean canDelete, boolean liked) {
        this.commentId = commentId;
        this.voteId = voteId;
        this.authorId = authorId;
        this.nickname = nickname != null ? nickname : Member.WITHDRAWN_NICKNAME;
        this.isBot = isBot != null && isBot;
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
        this.liked = liked;
    }

    /**
     * 현재 요청자의 좋아요 여부를 반영하는 메서드입니다.
     */
    public void updateLiked(boolean liked) {
        this.liked = liked;
    }
}
