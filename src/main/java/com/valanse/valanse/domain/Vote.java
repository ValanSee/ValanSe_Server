package com.valanse.valanse.domain;

import com.valanse.valanse.domain.enums.VoteCategory;
import com.valanse.valanse.domain.mapping.MemberVoteOption;
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
public class Vote {

    @Id
    private Long id;

    @Enumerated(EnumType.STRING)
    private VoteCategory category; // 예: 연애, 정치, 음식, 기타

    private String title;

    private Integer totalVoteCount;

    @OneToMany(mappedBy = "vote", cascade = CascadeType.ALL)
    private List<VoteOption> voteOptions = new ArrayList<>();

    @OneToOne(mappedBy = "vote", cascade = CascadeType.ALL)
    private CommentGroup commentGroup;
}

