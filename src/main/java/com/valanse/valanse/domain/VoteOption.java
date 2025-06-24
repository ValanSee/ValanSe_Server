package com.valanse.valanse.domain;

import com.valanse.valanse.domain.enums.VoteLabel;
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
public class VoteOption {

    @Id
    private Long id;

    private String content;

    private Integer voteCount;

    @Enumerated(EnumType.STRING)
    private VoteLabel label; // A, B, C, D

    @ManyToOne
    @JoinColumn(name = "vote_id")
    private Vote vote;

    @OneToMany(mappedBy = "voteOption", cascade = CascadeType.ALL)
    private List<MemberVoteOption> memberVoteOptions = new ArrayList<>();
}

