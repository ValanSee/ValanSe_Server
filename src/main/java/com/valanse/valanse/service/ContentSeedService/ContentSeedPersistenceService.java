package com.valanse.valanse.service.ContentSeedService;

import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.Vote;
import com.valanse.valanse.domain.VoteOption;
import com.valanse.valanse.domain.enums.VoteLabel;
import com.valanse.valanse.dto.Comment.CommentPostRequest;
import com.valanse.valanse.dto.Vote.VoteCreateRequest;
import com.valanse.valanse.repository.MemberRepository;
import com.valanse.valanse.repository.VoteRepository;
import com.valanse.valanse.service.CommentService.CommentService;
import com.valanse.valanse.service.VoteService.VoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 생성된 콘텐츠 1건(게시글 또는 상호작용)을 독립 트랜잭션으로 저장한다.
// 각 메서드가 REQUIRES_NEW이므로, 여러 건을 순서대로 저장하다가 뒤쪽에서 실패해도
// 앞서 커밋된 항목은 그대로 유지된다. 이 클래스는 외부(Claude) API를 호출하지
// 않는다 - 호출자가 생성까지 마친 뒤 결과 1건만 이 클래스로 넘겨야, DB 트랜잭션이
// 외부 API 호출 시간 동안 열려있지 않는다.
@Component
@RequiredArgsConstructor
public class ContentSeedPersistenceService {

    private final VoteService voteService;
    private final CommentService commentService;
    private final ContentSeedVotePersister votePersister;
    private final VoteRepository voteRepository;
    private final MemberRepository memberRepository;

    // 봇 게시글 1건 저장. 기존 VoteService.createVote를 재사용한다(작성 포인트는
    // PointService가 봇에게는 이미 지급하지 않는다).
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long saveBotPost(Long botMemberId, GeneratedPost post) {
        return voteService.createVote(botMemberId, toVoteCreateRequest(post));
    }

    // 봇 상호작용(투표 1건 + 댓글 1건) 저장. 투표는 ContentSeedVotePersister로
    // 부작용 없이, 댓글은 기존 CommentService.createComment로 저장한다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long saveBotInteraction(Long botMemberId, GeneratedInteraction interaction) {
        Member bot = memberRepository.findById(botMemberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 봇 회원입니다: " + botMemberId));
        Vote vote = voteRepository.findById(interaction.targetPostId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다: " + interaction.targetPostId()));
        VoteOption selectedOption = findOption(vote, interaction.selectedOption());

        votePersister.saveVote(bot, vote, selectedOption);

        return commentService.createComment(vote.getId(), botMemberId,
                CommentPostRequest.builder().content(interaction.comment()).build());
    }

    private VoteOption findOption(Vote vote, GeneratedInteraction.SelectedOption selected) {
        VoteLabel label = selected == GeneratedInteraction.SelectedOption.A ? VoteLabel.A : VoteLabel.B;
        return vote.getVoteOptions().stream()
                .filter(option -> option.getLabel() == label)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "게시글 " + vote.getId() + "에 " + label + " 선택지가 없습니다."));
    }

    private VoteCreateRequest toVoteCreateRequest(GeneratedPost post) {
        return VoteCreateRequest.builder()
                .title(post.title())
                .content(post.body())
                .category(post.category().toVoteCategory())
                .options(List.of(
                        VoteCreateRequest.OptionRequest.builder().content(post.optionA()).build(),
                        VoteCreateRequest.OptionRequest.builder().content(post.optionB()).build()
                ))
                .build();
    }
}
