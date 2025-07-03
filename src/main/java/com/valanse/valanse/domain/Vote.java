package com.valanse.valanse.domain;

import com.valanse.valanse.domain.common.BaseEntity;
import com.valanse.valanse.domain.enums.VoteCategory;
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
public class Vote extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private VoteCategory category; // 예: 연애, 음식, 기타

    private String title;

    private Integer totalVoteCount;

    @Builder.Default
    @OneToMany(mappedBy = "vote", cascade = CascadeType.ALL)
    private List<VoteOption> voteOptions = new ArrayList<>();

    @OneToOne(mappedBy = "vote", cascade = CascadeType.ALL)
    private CommentGroup commentGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    private boolean isDeleted;
}

