package com.valanse.valanse.service.VoteService;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.MemberProfile;
import com.valanse.valanse.domain.Vote;
import com.valanse.valanse.domain.VoteOption;
import com.valanse.valanse.dto.Vote.HotIssueVoteOptionDto;
import com.valanse.valanse.dto.Vote.HotIssueVoteResponse;
import com.valanse.valanse.repository.MemberProfileRepository;
import com.valanse.valanse.repository.MemberRepository;
import com.valanse.valanse.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 읽기 전용 트랜잭션 (조회 기능이므로)
public class VoteServiceImpl implements VoteService {

    private final VoteRepository voteRepository;
    private final MemberRepository memberRepository; // createdBy를 가져오기 위해 필요
    private final MemberProfileRepository memberProfileRepository; // createdBy (닉네임)를 가져오기 위해 필요

    @Override
    public HotIssueVoteResponse getHotIssueVote() {
        // 1. 가장 많이 참여한 투표 조회
        Vote hotIssueVote = voteRepository.findTopByOrderByTotalVoteCountDesc()
                .orElseThrow(() -> new ApiException("핫이슈 투표를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        // 2. 투표 생성자 정보 조회 (닉네임)
        // 투표 생성자가 Member 엔티티에 직접 연결되어 있지 않아, 여기서는 Member를 통해 Profile을 찾도록 가정합니다.
        // 만약 Vote 엔티티에 Member 연관 관계가 있다면 해당 필드를 통해 조회해야 합니다.
        // 현재 Vote 엔티티에 createdBy 정보가 없으므로, 편의상 특정 멤버의 닉네임을 가져오도록 임시로 작성합니다.
        // 실제 구현 시에는 Vote 엔티티에 'createdByMember'와 같은 필드를 추가하여 직접 연결하는 것이 좋습니다.
        String createdByNickname = null;
        if (hotIssueVote.getCommentGroup() != null && !hotIssueVote.getCommentGroup().getComments().isEmpty()) {
            // 예시: 첫 번째 댓글을 작성한 멤버의 닉네임을 가져옴 (실제 로직은 투표 생성자를 직접 참조해야 함)
            Member commentCreator = hotIssueVote.getCommentGroup().getComments().get(0).getMember();
            if (commentCreator != null) {
                MemberProfile profile = memberProfileRepository.findByMemberId(commentCreator.getId()).orElse(null);
                if (profile != null) {
                    createdByNickname = profile.getNickname();
                }
            }
        }
        // 만약 createdBy가 비어있을 경우 기본값 설정
        if (createdByNickname == null) {
            createdByNickname = "익명";
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
                .category(hotIssueVote.getCategory() != null ? hotIssueVote.getCategory().name() : null)
                .totalParticipants(hotIssueVote.getTotalVoteCount())
                .createdBy(createdByNickname)
                .options(options)
                .build();
    }
}