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

    // VoteServiceImpl.processVote와 동일하게 Vote -> VoteOption 순서로 비관적 락을
    // 걸고 엔티티를 다시 조회한다. 호출자가 넘긴 vote/selectedOption은 트랜잭션
    // 시작 전에 잠금 없이 읽힌 값일 수 있어, 그 값을 그대로 증가시키면 사용자 투표
    // 경로(processVote)와 동시에 실행될 때 두 트랜잭션이 같은 옛 카운트를 읽고
    // 써서 한쪽 증가분이 사라지는 Lost Update가 발생할 수 있다(#351).
    @Transactional
    public void saveVote(Member bot, Vote vote, VoteOption selectedOption) {
        Vote lockedVote = voteRepository.findByIdForUpdate(vote.getId())
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 투표입니다: " + vote.getId()));
        VoteOption lockedOption = voteOptionRepository.findByIdForUpdate(selectedOption.getId())
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 투표 선택지입니다: " + selectedOption.getId()));

        memberVoteOptionRepository.save(MemberVoteOption.builder()
                .member(bot)
                .vote(lockedVote)
                .voteOption(lockedOption)
                .build());

        lockedOption.setVoteCount(incrementCount(lockedOption.getVoteCount()));
        lockedVote.setTotalVoteCount(incrementCount(lockedVote.getTotalVoteCount()));
        voteOptionRepository.save(lockedOption);
        voteRepository.save(lockedVote);
    }

    private int incrementCount(Integer count) {
        return (count == null ? 0 : count) + 1;
    }
}
