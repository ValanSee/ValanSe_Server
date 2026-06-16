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
/**
 * CommentGroup 정보를 저장하고 연관관계를 표현하는 JPA 도메인 엔티티 코드입니다.
 */
public class CommentGroup extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "vote_id", unique = true)
    private Vote vote;

    @Builder.Default
    private Integer totalCommentCount = 0; // 기본값을 0으로 설정

    @Builder.Default
    @OneToMany(mappedBy = "commentGroup", cascade = CascadeType.ALL)
    private List<Comment> comments = new ArrayList<>();

    /**
     * CommentGroup의 setTotalCommentCount 기능을 수행하는 메서드입니다.
     */
    public void setTotalCommentCount(int count) {
        this.totalCommentCount = count;
    }

}

