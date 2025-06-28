package com.valanse.valanse.service.VoteService;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.MemberProfile;
import com.valanse.valanse.domain.Vote;
import com.valanse.valanse.domain.VoteOption;
import com.valanse.valanse.domain.mapping.MemberVoteOption;
import com.valanse.valanse.dto.Vote.HotIssueVoteOptionDto; // 기존 DTO 임포트
import com.valanse.valanse.dto.Vote.HotIssueVoteResponse; // 기존 DTO 임포트
import com.valanse.valanse.dto.Vote.VoteDetailResponse;
import com.valanse.valanse.dto.Vote.VoteResponseDto; // 새로 추가된 DTO 임포트
import com.valanse.valanse.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // import 추가

import java.time.LocalDateTime; // 기존 코드에 있었으므로 유지
import java.util.List; // 기존 코드에 있었으므로 유지
import java.util.Optional; // Optional 임포트 추가 (processVote 메서드에서 사용)
import java.util.stream.Collectors; // 기존 코드에 있었으므로 유지

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 클래스 레벨에서 기본적으로 읽기 전용 트랜잭션으로 설정
public class VoteServiceImpl implements VoteService {

    private final VoteRepository voteRepository;
    private final MemberProfileRepository memberProfileRepository; // MemberProfile 정보 조회를 위해 필요
    private final MemberRepository memberRepository; // processVote 메서드에서 Member 조회를 위해 추가
    private final VoteOptionRepository voteOptionRepository; // processVote 메서드에서 VoteOption 조회를 위해 추가
    private final MemberVoteOptionRepository memberVoteOptionRepository; // processVote 메서드에서 MemberVoteOption 조회를 위해 추가

    @Override
    public HotIssueVoteResponse getHotIssueVote() {
        // 1. 가장 많이 참여한 투표 조회 (totalVoteCount 기준, 동률일 경우 최신 생성일시 기준)
        Vote hotIssueVote = voteRepository.findTopByOrderByTotalVoteCountDescCreatedAtDesc()
                .orElseThrow(() -> new ApiException("핫이슈 투표를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        // 2. 투표 생성자 정보 조회 (닉네임)
        String createdByNickname = "익명"; // 기본값 설정
        if (hotIssueVote.getMember() != null) {
            Member creatorMember = hotIssueVote.getMember();
            MemberProfile profile = memberProfileRepository.findByMemberId(creatorMember.getId()).orElse(null);
            if (profile != null && profile.getNickname() != null) {
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
                .category(hotIssueVote.getCategory() != null ? hotIssueVote.getCategory().name() : null)
                .totalParticipants(hotIssueVote.getTotalVoteCount())
                .createdBy(createdByNickname)
                .options(options)
                .build();
    }

    @Override
    @Transactional // 이 메서드는 데이터를 변경하므로 읽기/쓰기 트랜잭션이 필요합니다. (클래스 레벨의 readOnly = true를 오버라이드)
    public VoteResponseDto processVote(Long userId, Long voteId, Long voteOptionId) {
        // 1. 필수 엔티티들을 조회합니다. (없으면 예외 발생)
        Member member = memberRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException("회원이 존재하지 않습니다.", HttpStatus.NOT_FOUND));

        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new ApiException("투표가 존재하지 않습니다.", HttpStatus.NOT_FOUND));

        VoteOption newVoteOption = voteOptionRepository.findById(voteOptionId)
                .orElseThrow(() -> new ApiException("투표 선택지가 존재하지 않습니다.", HttpStatus.NOT_FOUND));

        // 2. 사용자가 이 투표에 대해 이전에 투표한 선택지가 있는지 확인합니다.
        // ERD의 member_vote_option 테이블을 활용 [cite: image_02691a.jpg]
        Optional<MemberVoteOption> existingVote = memberVoteOptionRepository.findByMemberIdAndVoteId(userId, voteId);

        boolean isVoted; // 최종적으로 투표가 되어 있는지 여부 (응답 DTO에 사용)
        Integer updatedTotalVoteCount; // 업데이트된 총 투표 수 (응답 DTO에 사용)
        Integer updatedVoteOptionCount; // 업데이트된 선택지별 투표 수 (응답 DTO에 사용)

        if (existingVote.isPresent()) {
            // 3. 이전에 투표한 기록이 있는 경우 (수정 또는 취소)
            MemberVoteOption oldMemberVoteOption = existingVote.get();
            VoteOption oldVoteOption = oldMemberVoteOption.getVoteOption(); // 이전에 투표했던 선택지

            if (oldVoteOption.getId().equals(voteOptionId)) {
                // 3-1. 동일한 선택지를 다시 클릭한 경우: 투표 취소
                // member_vote_option에서 해당 기록을 삭제 [cite: image_0268de.png]
                memberVoteOptionRepository.delete(oldMemberVoteOption);
                // 기존 선택지의 투표 수 감소 [cite: image_0268de.png]
                oldVoteOption.setVoteCount(oldVoteOption.getVoteCount() - 1);
                // 전체 투표 수 감소 [cite: image_0268de.png]
                vote.setTotalVoteCount(vote.getTotalVoteCount() - 1);

                isVoted = false; // 투표가 취소되었으므로 false
                updatedTotalVoteCount = vote.getTotalVoteCount();
                updatedVoteOptionCount = oldVoteOption.getVoteCount();

            } else {
                // 3-2. 다른 선택지를 클릭한 경우: 기존 투표 취소 후 새 투표 기록 (재선택)
                // 기존 member_vote_option 기록 삭제
                memberVoteOptionRepository.delete(oldMemberVoteOption);
                // 기존 선택지의 투표 수 감소
                oldVoteOption.setVoteCount(oldVoteOption.getVoteCount() - 1);

                // 새로운 member_vote_option 기록 생성 및 저장
                MemberVoteOption newMemberVoteOption = MemberVoteOption.builder()
                        .member(member)
                        .vote(vote)
                        .voteOption(newVoteOption)
                        .build();
                memberVoteOptionRepository.save(newMemberVoteOption);
                // 새로운 선택지의 투표 수 증가
                newVoteOption.setVoteCount(newVoteOption.getVoteCount() + 1);

                // 전체 투표수는 변동 없음 (기존 1 감소, 새로운 1 증가)
                isVoted = true; // 새로운 옵션에 투표했으므로 true
                updatedTotalVoteCount = vote.getTotalVoteCount();
                updatedVoteOptionCount = newVoteOption.getVoteCount();
            }
        } else {
            // 4. 이전에 투표한 기록이 없는 경우: 새로운 투표 기록
            // 새로운 member_vote_option 기록 생성 및 저장
            MemberVoteOption newMemberVoteOption = MemberVoteOption.builder()
                    .member(member)
                    .vote(vote)
                    .voteOption(newVoteOption)
                    .build();
            memberVoteOptionRepository.save(newMemberVoteOption);
            // 선택지 투표 수 증가
            newVoteOption.setVoteCount(newVoteOption.getVoteCount() + 1);
            // 전체 투표 수 증가
            vote.setTotalVoteCount(vote.getTotalVoteCount() + 1);

            isVoted = true; // 새로운 옵션에 투표했으므로 true
            updatedTotalVoteCount = vote.getTotalVoteCount();
            updatedVoteOptionCount = newVoteOption.getVoteCount();
        }

        // 5. 변경된 엔티티들을 데이터베이스에 저장합니다.
        // @Transactional 덕분에 이 변경사항들이 한 번에 커밋되거나 롤백됩니다.
        voteRepository.save(vote);
        voteOptionRepository.save(newVoteOption); // 새 투표 옵션은 항상 변경되므로 저장

        // 만약 이전 투표 옵션이 있었고, 다른 옵션으로 재선택한 경우라면, 이전 옵션의 voteCount도 저장해야 합니다.
        if (existingVote.isPresent() && !existingVote.get().getVoteOption().getId().equals(voteOptionId)) {
            voteOptionRepository.save(existingVote.get().getVoteOption());
        }

        // 6. 결과 DTO를 구성하여 반환합니다.
        return new VoteResponseDto(
                isVoted,
                updatedTotalVoteCount,
                voteOptionId, // 현재 작업의 대상이 된 voteOptionId
                updatedVoteOptionCount
        );
    }
    @Override
    public VoteDetailResponse getVoteDetailById(Long voteId) {
        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new ApiException("투표를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        // MemberProfile의 nickname을 가져오도록 수정
        String creatorNickname = null;
        if (vote.getMember() != null && vote.getMember().getProfile() != null) {
            creatorNickname = vote.getMember().getProfile().getNickname();
        }

        List<VoteDetailResponse.VoteOptionDto> optionDtos = vote.getVoteOptions().stream()
                .map(option -> VoteDetailResponse.VoteOptionDto.builder()
                        .optionId(option.getId())
                        .content(option.getContent())
                        .voteCount(option.getVoteCount())
                        .label(option.getLabel().name())
                        .build())
                .collect(Collectors.toList());

        return VoteDetailResponse.builder()
                .voteId(vote.getId())
                .title(vote.getTitle())
                .category(vote.getCategory())
                .totalVoteCount(vote.getTotalVoteCount())
                .creatorNickname(creatorNickname) // 수정된 닉네임 사용
                .createdAt(vote.getCreatedAt())
                .options(optionDtos)
                .build();
    }

    /*
    // TODO: 추후 특정 기간 내의 핫이슈 투표를 가져올 필요가 있을 때 활용할 수 있는 메서드 (datetime 고려)
    // 이 메서드를 활성화하려면 VoteRepository에 Optional<Vote> findTopByCreatedAtAfterOrderByTotalVoteCountDescCreatedAtDesc(LocalDateTime createdAt); 정의 필요
    // 이 메서드는 조회 기능이므로 @Transactional(readOnly = true)를 유지합니다.
    public HotIssueVoteResponse getHotIssueVoteWithinPeriod(LocalDateTime startDate) {
        Vote hotIssueVote = voteRepository.findTopByCreatedAtAfterOrderByTotalVoteCountDescCreatedAtDesc(startDate)
                .orElseThrow(() -> new ApiException("해당 기간의 핫이슈 투표를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        String createdByNickname = "익명";
        if (hotIssueVote.getMember() != null) {
            Member creatorMember = hotIssueVote.getMember();
            MemberProfile profile = memberProfileRepository.findByMemberId(creatorMember.getId()).orElse(null);
            if (profile != null && profile.getNickname() != null) {
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
                .category(hotIssueVote.getCategory() != null ? hotIssueVote.getCategory().name() : null)
                .totalParticipants(hotIssueVote.getTotalVoteCount())
                .createdBy(createdByNickname)
                .options(options)
                .build();
    }
    */
}