package com.valanse.valanse.domain.mapping;

import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.Vote;
import com.valanse.valanse.domain.VoteOption;
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
public class MemberVoteOption {

    @Id
    private Long id;

    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne
    @JoinColumn(name = "vote_id")
    private Vote vote;

    @ManyToOne
    @JoinColumn(name = "vote_option_id")
    private VoteOption voteOption;
}

