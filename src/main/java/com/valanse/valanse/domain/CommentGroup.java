package com.valanse.valanse.domain;

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
public class CommentGroup {

    @Id
    private Long id;

    @OneToOne
    @JoinColumn(name = "vote_id")
    private Vote vote;

    private Integer totalCommentCount;

    @OneToMany(mappedBy = "commentGroup", cascade = CascadeType.ALL)
    private List<Comment> comments = new ArrayList<>();
}

