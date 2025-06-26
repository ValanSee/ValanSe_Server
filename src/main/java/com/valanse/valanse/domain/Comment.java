package com.valanse.valanse.domain;

import com.valanse.valanse.domain.common.BaseEntity;
import com.valanse.valanse.domain.mapping.CommentLike;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String content;

    private Integer likeCount;

    private Integer replyCount; // 대댓글 개수

    private Boolean isDeleted;

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

    @Builder.Default
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private List<Comment> replies = new ArrayList<>();

    public void updateReplyCount(int newCount) {
        this.replyCount = newCount;
    }

}

