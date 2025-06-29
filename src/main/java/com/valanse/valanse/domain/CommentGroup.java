package com.valanse.valanse.domain;

import com.valanse.valanse.domain.common.BaseEntity;
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
public class CommentGroup extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "vote_id")
    private Vote vote;

    @Builder.Default
    private Integer totalCommentCount = 0;

    @Builder.Default
    @OneToMany(mappedBy = "commentGroup", cascade = CascadeType.ALL)
    private List<Comment> comments = new ArrayList<>();

    public void setTotalCommentCount(int count) {
        this.totalCommentCount = count;
    }

}

