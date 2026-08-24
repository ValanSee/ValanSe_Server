package com.valanse.valanse.service.BotVoteService;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.common.message.MemberErrorMessage;
import com.valanse.valanse.common.message.VoteErrorMessage;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.VoteOption;
import com.valanse.valanse.domain.mapping.MemberVoteOption;
import com.valanse.valanse.repository.MemberRepository;
import com.valanse.valanse.repository.MemberVoteOptionRepository;
import com.valanse.valanse.repository.VoteOptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class BotVoteServiceImpl implements BotVoteService {

    private final MemberRepository memberRepository;
    private final VoteOptionRepository voteOptionRepository;
    private final MemberVoteOptionRepository memberVoteOptionRepository;

    @Override
    public void castBotVote(Long botMemberId, Long voteOptionId) {
        Member member = memberRepository.findByIdAndDeletedAtIsNull(botMemberId)
                .orElseThrow(() -> new ApiException(MemberErrorMessage.MEMBER_NOT_FOUND.message(), HttpStatus.NOT_FOUND));

        VoteOption voteOption = voteOptionRepository.findById(voteOptionId)
                .orElseThrow(() -> new ApiException(VoteErrorMessage.VOTE_OPTION_NOT_FOUND.message(), HttpStatus.NOT_FOUND));

        MemberVoteOption memberVoteOption = MemberVoteOption.builder()
                .member(member)
                .vote(voteOption.getVote())
                .voteOption(voteOption)
                .build();

        try {
            memberVoteOptionRepository.save(memberVoteOption);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(VoteErrorMessage.VOTE_ALREADY_PROCESSED.message(), HttpStatus.BAD_REQUEST);
        }
    }
}
