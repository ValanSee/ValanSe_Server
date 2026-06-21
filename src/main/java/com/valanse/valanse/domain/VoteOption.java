package com.valanse.valanse.domain;

import com.valanse.valanse.domain.common.BaseEntity;
import com.valanse.valanse.domain.enums.VoteLabel;
import com.valanse.valanse.domain.mapping.MemberVoteOption;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
/**
 * VoteOption 정보를 저장하고 연관관계를 표현하는 JPA 도메인 엔티티 코드입니다.
 */
public class VoteOption extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String content;

    @Builder.Default // voteCount 초기값 0 설정
    private Integer voteCount = 0;

    @Enumerated(EnumType.STRING)
    private VoteLabel label; // A, B, C, D

    @ManyToOne
    @JoinColumn(name = "vote_id")
    private Vote vote;

    @Builder.Default
    @OneToMany(mappedBy = "voteOption", cascade = CascadeType.ALL)
    private List<MemberVoteOption> memberVoteOptions = new ArrayList<>();

    // 편의 메서드: Vote 설정 (양방향 관계를 위해 필요)
    /**
     * VoteOption의 setVote 기능을 수행하는 메서드입니다.
     */
    public void setVote(Vote vote) {
        this.vote = vote;
    }
}

