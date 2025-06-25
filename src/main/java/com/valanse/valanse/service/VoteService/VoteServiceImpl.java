// src/main/java/com/valanse/valanse/service/VoteService/VoteServiceImpl.java
package com.valanse.valanse.service.VoteService;

import com.valanse.valanse.common.api.ApiException; //
import com.valanse.valanse.domain.Member; //
import com.valanse.valanse.domain.MemberProfile; //
import com.valanse.valanse.domain.Vote; //
import com.valanse.valanse.dto.Vote.HotIssueVoteOptionDto;
import com.valanse.valanse.dto.Vote.HotIssueVoteResponse;
import com.valanse.valanse.repository.MemberProfileRepository; //
import com.valanse.valanse.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus; //
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 조회 기능이므로 읽기 전용 트랜잭션
public class VoteServiceImpl implements VoteService {

    private final VoteRepository voteRepository;
    private final MemberProfileRepository memberProfileRepository; // MemberProfile 정보 조회를 위해 필요

    @Override
    public HotIssueVoteResponse getHotIssueVote() {
        // 1. 가장 많이 참여한 투표 조회 (totalVoteCount 기준, 동률일 경우 최신 생성일시 기준)
        Vote hotIssueVote = voteRepository.findTopByOrderByTotalVoteCountDescCreatedAtDesc() //
                .orElseThrow(() -> new ApiException("핫이슈 투표를 찾을 수 없습니다.", HttpStatus.NOT_FOUND)); //

        // 2. 투표 생성자 정보 조회 (닉네임)
        // Vote 엔티티의 'member' 필드를 사용하여 직접 Member 객체에 접근
        String createdByNickname = "익명"; // 기본값 설정
        if (hotIssueVote.getMember() != null) { // 'member' 필드 사용
            Member creatorMember = hotIssueVote.getMember(); //
            MemberProfile profile = memberProfileRepository.findByMemberId(creatorMember.getId()).orElse(null); //
            if (profile != null && profile.getNickname() != null) { //
                createdByNickname = profile.getNickname();
            } else {
                // MemberProfile이 없거나 닉네임이 없는 경우, Member의 기본 이름 사용 (카카오 이름 등)
                if (creatorMember.getName() != null) {
                    createdByNickname = creatorMember.getName();
                }
            }
        }

        // 3. 투표 옵션 정보 DTO로 변환
        List<HotIssueVoteOptionDto> options = hotIssueVote.getVoteOptions().stream()
                .map(option -> HotIssueVoteOptionDto.builder()
                        .content(option.getContent())
                        .vote_count(option.getVoteCount())
                        .build())
                .collect(Collectors.toList());

        // 4. HotIssueVoteResponse DTO 생성 및 반환
        return HotIssueVoteResponse.builder()
                .voteId(hotIssueVote.getId())
                .title(hotIssueVote.getTitle())
                .category(hotIssueVote.getCategory() != null ? hotIssueVote.getCategory().name() : null) //
                .totalParticipants(hotIssueVote.getTotalVoteCount())
                .createdBy(createdByNickname)
                .options(options)
                .build();
    }

    /*
    // TODO: 추후 특정 기간 내의 핫이슈 투표를 가져올 필요가 있을 때 활용할 수 있는 메서드 (datetime 고려)
    // 이 메서드를 활성화하려면 VoteRepository에 Optional<Vote> findTopByCreatedAtAfterOrderByTotalVoteCountDescCreatedAtDesc(LocalDateTime createdAt); 정의 필요
    public HotIssueVoteResponse getHotIssueVoteWithinPeriod(LocalDateTime startDate) {
        Vote hotIssueVote = voteRepository.findTopByCreatedAtAfterOrderByTotalVoteCountDescCreatedAtDesc(startDate) //
                .orElseThrow(() -> new ApiException("해당 기간의 핫이슈 투표를 찾을 수 없습니다.", HttpStatus.NOT_FOUND)); //

        String createdByNickname = "익명";
        if (hotIssueVote.getMember() != null) { // 'member' 필드 사용
            Member creatorMember = hotIssueVote.getMember(); //
            MemberProfile profile = memberProfileRepository.findByMemberId(creatorMember.getId()).orElse(null); //
            if (profile != null && profile.getNickname() != null) { //
                createdByNickname = profile.getNickname();
            } else {
                if (creatorMember.getName() != null) {
                    createdByNickname = creatorMember.getName();
                }
            }
        }

        List<HotIssueVoteOptionDto> options = hotIssueVote.getVoteOptions().stream()
                .map(option -> HotIssueVoteOptionDto.builder()
                        .content(option.getContent())
                        .vote_count(option.getVoteCount())
                        .build())
                .collect(Collectors.toList());

        return HotIssueVoteResponse.builder()
                .voteId(hotIssueVote.getId())
                .title(hotIssueVote.getTitle())
                .category(hotIssueVote.getCategory() != null ? hotIssueVote.getCategory().name() : null) //
                .totalParticipants(hotIssueVote.getTotalVoteCount())
                .createdBy(createdByNickname)
                .options(options)
                .build();
    }
    */
}