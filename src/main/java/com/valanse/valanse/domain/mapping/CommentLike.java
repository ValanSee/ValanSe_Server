package com.valanse.valanse.domain.mapping;

import com.valanse.valanse.domain.Comment;
import com.valanse.valanse.domain.Member;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentLike {

    @Id
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private Member user;

    @ManyToOne
    @JoinColumn(name = "comment_id")
    private Comment comment;
}

