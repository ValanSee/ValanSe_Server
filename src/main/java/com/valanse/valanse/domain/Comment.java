package com.valanse.valanse.domain;

import com.valanse.valanse.domain.common.BaseEntity;
import com.valanse.valanse.domain.mapping.CommentLike;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
/**
 * Comment 정보를 저장하고 연관관계를 표현하는 JPA 도메인 엔티티 코드입니다.
 */
public class Comment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255)
    private String title;  // 댓글 제목 추가

    @Column(columnDefinition = "TEXT")
    private String content;

    private Integer likeCount;

    private Integer replyCount; // 대댓글 개수

    private LocalDateTime deletedAt;

    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member; // Comment : Member = N : 1

    @ManyToOne
    @JoinColumn(name = "comment_group_id")
    private CommentGroup commentGroup; // Comment : CommentGroup = N : 1

    @Builder.Default
    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL)
    private List<CommentLike> likes = new ArrayList<>(); // Comment : CommentLike = 1 : N

    // Comment(replies) : Comment(parent) = N : 1
    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @ManyToOne
    @JoinColumn(name = "vote_option_id")
    private VoteOption voteOption;

    @Builder.Default
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private List<Comment> replies = new ArrayList<>();

    /**
     * ReplyCount 데이터를 수정하는 메서드입니다.
     */
    public void updateReplyCount(int newCount) {
        this.replyCount = newCount;
    }

    /**
     * Comment의 setLikeCount 기능을 수행하는 메서드입니다.
     */
    public void setLikeCount(int count) {
        this.likeCount = count;
    }

    /**
     * Comment의 setDeletedAt 기능을 수행하는 메서드입니다.
     */
    public void setDeletedAt(LocalDateTime deletedAt) {  // 메서드 변경
        this.deletedAt = deletedAt;
    }
}

