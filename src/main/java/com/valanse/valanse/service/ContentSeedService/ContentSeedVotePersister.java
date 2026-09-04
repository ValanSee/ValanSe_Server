package com.valanse.valanse.service.ContentSeedService;

import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.Vote;
import com.valanse.valanse.domain.VoteOption;
import com.valanse.valanse.domain.mapping.MemberVoteOption;
import com.valanse.valanse.repository.MemberVoteOptionRepository;
import com.valanse.valanse.repository.VoteOptionRepository;
import com.valanse.valanse.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// 봇의 상호작용 투표를 저장한다. VoteServiceImpl.processVote와 달리 PointService는
// 호출하지 않는다 - 봇 투표로 게시물 작성자가 공짜 포인트를 받으면 안 되기 때문이다.
// 다만 Vote.totalVoteCount/VoteOption.voteCount는 함께 증가시켜, 화면의 참여 수·
// 투표율에도 봇 활동이 반영되도록 한다.
// 댓글의 "선택한 옵션" 표시는 CommentRepositoryImpl이 이 MemberVoteOption을 조인해서
// 보여주므로, 이 메서드로 저장하는 것만으로 표시가 유지된다.
@Component
@RequiredArgsConstructor
public class ContentSeedVotePersister {

    private final MemberVoteOptionRepository memberVoteOptionRepository;
    private final VoteRepository voteRepository;
    private final VoteOptionRepository voteOptionRepository;

    // VoteServiceImpl.processVote와 마찬가지로, 카운트를 바꾼 뒤 명시적으로 save한다 -
    // 넘어온 vote/selectedOption이 (호출자에 따라) 이미 detach된 엔티티일 수 있어,
    // 필드만 바꾸고 flush에 기대면 반영되지 않을 수 있기 때문이다.
    @Transactional
    public void saveVote(Member bot, Vote vote, VoteOption selectedOption) {
        memberVoteOptionRepository.save(MemberVoteOption.builder()
                .member(bot)
                .vote(vote)
                .voteOption(selectedOption)
                .build());

        selectedOption.setVoteCount(incrementCount(selectedOption.getVoteCount()));
        vote.setTotalVoteCount(incrementCount(vote.getTotalVoteCount()));
        voteOptionRepository.save(selectedOption);
        voteRepository.save(vote);
    }

    private int incrementCount(Integer count) {
        return (count == null ? 0 : count) + 1;
    }
}
