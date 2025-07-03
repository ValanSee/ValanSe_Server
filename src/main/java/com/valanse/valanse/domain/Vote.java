package com.valanse.valanse.domain;

import com.valanse.valanse.domain.common.BaseEntity;
import com.valanse.valanse.domain.enums.VoteCategory;
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

    @Builder.Default
    private Integer totalVoteCount = 0; //Builder.Default 설정이 없으면 null값이 기본값이라 오류가 발생한다.

    @Builder.Default
    @OneToMany(mappedBy = "vote", cascade = CascadeType.ALL)
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

    // 편의 메서드: VoteOption 추가
    public void addVoteOption(VoteOption voteOption) {
        this.voteOptions.add(voteOption);
        voteOption.setVote(this);
    }

}

