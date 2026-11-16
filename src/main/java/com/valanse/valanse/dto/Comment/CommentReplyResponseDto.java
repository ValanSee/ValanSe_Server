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
    // 대댓글 작성자 회원 id (차단하기용). 탈퇴 후 퍼지된 작성자면 null
    private Long authorId;
    private String nickname;
    private Boolean isBot;
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
    // 현재 요청자(로그인 사용자)가 이 대댓글에 좋아요를 눌렀는지 여부. 비로그인이면 항상 false
    private boolean liked;

    /**
     * QueryDSL 조회용 생성자입니다. (작성자 id 포함)
     */
    @QueryProjection
    public CommentReplyResponseDto(Long id, Long authorId, String nickname, Boolean isBot, String title,
                                   LocalDateTime createdAt, String content, Integer likeCount, Integer replyCount,
                                   Boolean isDeleted, LocalDateTime deletedAt, VoteLabel label,
                                   Long daysAgo, Long hoursAgo, Boolean canDelete) {
        this(id, authorId, nickname, isBot, title, createdAt, content, likeCount, replyCount, isDeleted, deletedAt,
                label, daysAgo, hoursAgo, canDelete, false);
    }

    /**
     * 작성자 id 없이 생성하는 기존 호환용 생성자입니다.
     */
    public CommentReplyResponseDto(Long id, String nickname, Boolean isBot, String title, LocalDateTime createdAt,
                                   String content, Integer likeCount, Integer replyCount,
                                   Boolean isDeleted, LocalDateTime deletedAt, VoteLabel label,
                                   Long daysAgo, Long hoursAgo, Boolean canDelete) {
        this(id, null, nickname, isBot, title, createdAt, content, likeCount, replyCount, isDeleted, deletedAt, label,
                daysAgo, hoursAgo, canDelete, false);
    }

    /**
     * 작성자 id 없이 liked 값을 포함해 생성하는 기존 호환용 생성자입니다.
     */
    public CommentReplyResponseDto(Long id, String nickname, Boolean isBot, String title, LocalDateTime createdAt,
                                   String content, Integer likeCount, Integer replyCount,
                                   Boolean isDeleted, LocalDateTime deletedAt, VoteLabel label,
                                   Long daysAgo, Long hoursAgo, Boolean canDelete, boolean liked) {
        this(id, null, nickname, isBot, title, createdAt, content, likeCount, replyCount, isDeleted, deletedAt, label,
                daysAgo, hoursAgo, canDelete, liked);
    }

    /**
     * 전체 필드 생성자입니다. (@Builder 용)
     */
    public CommentReplyResponseDto(Long id, Long authorId, String nickname, Boolean isBot, String title,
                                   LocalDateTime createdAt, String content, Integer likeCount, Integer replyCount,
                                   Boolean isDeleted, LocalDateTime deletedAt, VoteLabel label,
                                   Long daysAgo, Long hoursAgo, Boolean canDelete, boolean liked) {
        this.id = id;
        this.authorId = authorId;
        this.nickname = nickname != null ? nickname : "탈퇴한 사용자";
        this.isBot = isBot != null && isBot;
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
        this.liked = liked;
    }

    /**
     * 현재 요청자의 좋아요 여부를 반영하는 메서드입니다.
     */
    public void updateLiked(boolean liked) {
        this.liked = liked;
    }
}
