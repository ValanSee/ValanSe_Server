package com.valanse.valanse.service.ContentSeedService;

import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.Vote;
import com.valanse.valanse.domain.VoteOption;
import com.valanse.valanse.domain.mapping.MemberVoteOption;
import com.valanse.valanse.repository.MemberVoteOptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// 봇의 상호작용 투표를 부작용 없이 저장한다. VoteServiceImpl.processVote와 달리
// Vote.totalVoteCount/VoteOption.voteCount를 증가시키지 않고 PointService도 호출하지
// 않는다 - 봇 활동이 공개 투표 카운트나 작성자 포인트에 영향을 주면 안 되기 때문이다.
// 댓글의 "선택한 옵션" 표시는 CommentRepositoryImpl이 이 MemberVoteOption을 조인해서
// 보여주므로, 이 메서드로 저장하는 것만으로 표시가 유지된다.
@Component
@RequiredArgsConstructor
public class ContentSeedVotePersister {

    private final MemberVoteOptionRepository memberVoteOptionRepository;

    @Transactional
    public void saveVote(Member bot, Vote vote, VoteOption selectedOption) {
        memberVoteOptionRepository.save(MemberVoteOption.builder()
                .member(bot)
                .vote(vote)
                .voteOption(selectedOption)
                .build());
    }
}
