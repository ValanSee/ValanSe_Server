package com.valanse.valanse.domain;

import com.valanse.valanse.domain.common.BaseEntity;
import com.valanse.valanse.domain.enums.VoteCategory;
import com.valanse.valanse.domain.mapping.MemberVoteOption;
import jakarta.persistence.*;
import lombok.*;

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

    @OneToMany(mappedBy = "vote", cascade = CascadeType.ALL)
    @Builder.Default // ArrayList 초기화를 위해서 영서가 추가.
    private List<VoteOption> voteOptions = new ArrayList<>();

    @OneToOne(mappedBy = "vote", cascade = CascadeType.ALL)
    private CommentGroup commentGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // totalVoteCount에 대한 Setter 추가
    public void setTotalVoteCount(Integer totalVoteCount) {
        this.totalVoteCount = totalVoteCount;
    }

}

