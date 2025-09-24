package com.valanse.valanse.service.VoteService;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.domain.*;
import com.valanse.valanse.domain.enums.VoteCategory;
import com.valanse.valanse.domain.enums.VoteLabel;
import com.valanse.valanse.domain.mapping.MemberVoteOption;
import com.valanse.valanse.dto.Vote.*;
import com.valanse.valanse.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // import 추가

import java.time.LocalDateTime; // 기존 코드에 있었으므로 유지
import java.util.List; // 기존 코드에 있었으므로 유지
import java.util.Optional; // Optional 임포트 추가 (processVote 메서드에서 사용)
import java.util.stream.Collectors; // 기존 코드에 있었으므로 유지

@Service
@RequiredArgsConstructor
@Transactional // 클래스 레벨에서 기본적으로 읽기 전용 트랜잭션으로 설정
public class VoteServiceImpl implements VoteService {

    private final VoteRepository voteRepository;
    private final MemberProfileRepository memberProfileRepository; // MemberProfile 정보 조회를 위해 필요
    private final MemberRepository memberRepository; // processVote 메서드에서 Member 조회를 위해 추가
    private final VoteOptionRepository voteOptionRepository; // processVote 메서드에서 VoteOption 조회를 위해 추가
    private final MemberVoteOptionRepository memberVoteOptionRepository; // processVote 메서드에서 MemberVoteOption 조회를 위해 추가
    private final CommentGroupRepository commentGroupRepository;

   //작은 민지가 구현한 것
   @Override
   public List<VoteResponseDto> getMyCreatedVotes(Long memberId, String sort, VoteCategory category) {
       Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
               .orElseThrow(() -> new ApiException("회원이 존재하지 않습니다.", HttpStatus.NOT_FOUND));

       List<Vote> votes;

       boolean isAllCategory = category == VoteCategory.ALL;

       if (isAllCategory) {
           votes = sort.equals("latest") ?
                   voteRepository.findAllByMemberOrderByCreatedAtDesc(member) :
                   voteRepository.findAllByMemberOrderByCreatedAtAsc(member);
       } else {
           if (category == null)
               throw new ApiException("카테고리를 입력해주세요.", HttpStatus.BAD_REQUEST);

           votes = sort.equals("latest") ?
                   voteRepository.findAllByMemberAndCategoryOrderByCreatedAtDesc(member, category) :
                   voteRepository.findAllByMemberAndCategoryOrderByCreatedAtAsc(member, category);
       }

       return votes.stream().map(VoteResponseDto::new).collect(Collectors.toList());
   }

    @Override
    public List<VoteResponseDto> getMyVotedVotes(Long memberId, String sort, VoteCategory category) {
        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new ApiException("회원이 존재하지 않습니다.", HttpStatus.NOT_FOUND));

        List<Vote> votes;

        boolean isAllCategory = category == VoteCategory.ALL;

        if (isAllCategory) {
            votes = sort.equals("latest") ?
                    voteRepository.findAllByMemberVotedOrderByCreatedAtDesc(member) :
                    voteRepository.findAllByMemberVotedOrderByCreatedAtAsc(member);
        } else {
            if (category == null)
                throw new ApiException("카테고리를 입력해주세요.", HttpStatus.BAD_REQUEST);

            votes = sort.equals("latest") ?
                    voteRepository.findAllByMemberVotedAndCategoryOrderByCreatedAtDesc(member, category) :
                    voteRepository.findAllByMemberVotedAndCategoryOrderByCreatedAtAsc(member, category);
        }

        return votes.stream().map(VoteResponseDto::new).collect(Collectors.toList());
    }

    //여기서부터 영서 코드
    @Override
    public HotIssueVoteResponse getHotIssueVote() { // 파라미터 없음
        // 수정: 작일 반응성 기준으로 변경
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterdayStart = now.minusDays(1).withHour(0).withMinute(0).withSecond(0);

        // 1. 먼저 모든 투표의 반응성 점수를 업데이트 (실시간 계산)
        List<Vote> allVotes = voteRepository.findAll();
        for(Vote vote : allVotes) {
            vote.updateReactivityScore(); // Vote 엔티티에 추가한 메서드
            voteRepository.save(vote);
        }

        // 2. 작일 동안 반응성이 가장 높은 투표 조회 시도
        Optional<Vote> yesterdayHotIssue = voteRepository
                .findTopByReactivityUpdatedAtBetweenOrderByReactivityScoreDescCreatedAtDesc(yesterdayStart, now);

        Vote hotIssueVote;
        if (yesterdayHotIssue.isPresent()) {
            // 작일 반응성 데이터가 있는 경우
            hotIssueVote = yesterdayHotIssue.get();
        } else {
            // 작일 반응성 데이터가 없는 경우 → 전체 누적 반응성 기준 조회
            hotIssueVote = voteRepository.findTopByOrderByReactivityScoreDescCreatedAtDesc()
                    .orElseThrow(() -> new ApiException("핫이슈 투표를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        }

        // 3. 투표 생성자 정보 조회 (닉네임) - 기존 로직 유지
        String createdByNickname = "익명"; // 기본값 설정
        if (hotIssueVote.getMember() != null) { // Member가 null이 아닌 경우
            Member creatorMember = hotIssueVote.getMember(); // 생성자 Member 객체 가져오기
            MemberProfile profile = memberProfileRepository.findByMemberId(creatorMember.getId()).orElse(null); // MemberProfile 조회
            if (profile != null && profile.getNickname() != null) { // 프로필이 있고 닉네임이 있는 경우
                createdByNickname = profile.getNickname(); // 닉네임 설정
            } else {
                // MemberProfile이 없거나 닉네임이 없는 경우, Member의 기본 이름 사용 (카카오 이름 등)
                if (creatorMember.getName() != null) { // Member의 이름이 있는 경우
                    createdByNickname = creatorMember.getName(); // 이름으로 닉네임 설정
                }
            }
        }

        // 4. 투표 옵션 정보 DTO로 변환
        List<HotIssueVoteOptionDto> options = hotIssueVote.getVoteOptions().stream()
                .map(option -> HotIssueVoteOptionDto.builder() // HotIssueVoteOptionDto 빌더 사용
                        .optionId(option.getId())  //option ID넣기
                        .content(option.getContent()) // 옵션 내용 설정
                        .vote_count(option.getVoteCount()) // 투표 수 설정
                        .build())
                .collect(Collectors.toList()); // 리스트로 수집

        // 5. HotIssueVoteResponse DTO 생성 및 반환
        return HotIssueVoteResponse.builder()
                .voteId(hotIssueVote.getId()) // 투표 ID 설정
                .title(hotIssueVote.getTitle()) // 제목 설정
                .category(hotIssueVote.getCategory() != null ? hotIssueVote.getCategory().name() : null) // 카테고리 설정
                .totalParticipants(hotIssueVote.getTotalVoteCount()) // 총 참여자 수 설정
                .createdBy(createdByNickname) // 생성자 닉네임 설정
                .createdAt(hotIssueVote.getCreatedAt()) // 추가된 부분: createdAt 설정
                .options(options) // 옵션 리스트 설정
                .build();
    }

    @Override
    @Transactional // 이 메서드는 데이터를 변경하므로 읽기/쓰기 트랜잭션이 필요합니다. (클래스 레벨의 readOnly = true를 오버라이드)
    public VoteCancleResponseDto processVote(Long userId, Long voteId, Long voteOptionId) {
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
        return new VoteCancleResponseDto(
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
        // --- 추가된 로직 시작 ---
        Boolean hasVoted = false;
        String votedOptionLabel = null;

        // 현재 로그인한 사용자 ID 가져오기
        // JwtTokenFilter에서 인증 시 SecurityContextHolder에 userId를 String으로 저장함
        Long currentUserId = null;
        try {
            if (SecurityContextHolder.getContext().getAuthentication() != null &&
                    SecurityContextHolder.getContext().getAuthentication().getName() != null &&
                    !SecurityContextHolder.getContext().getAuthentication().getName().equals("anonymousUser")) {
                currentUserId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
                // --- 디버깅 로그 추가 시작 ---
                System.out.println("DEBUG: Authenticated user ID: " + currentUserId);
                // --- 디버깅 로그 추가 끝 ---
            } else {
                // --- 디버깅 로그 추가 시작 ---
                System.out.println("DEBUG: User is not authenticated or is anonymous.");
                // --- 디버깅 로그 추가 끝 ---
            }
        } catch (NumberFormatException e) {
            System.out.println("DEBUG: Error parsing user ID from SecurityContext: " + e.getMessage());
            currentUserId = null;
        }


        if (currentUserId != null) {
            // 사용자의 투표 기록 조회
            Optional<MemberVoteOption> userVote = memberVoteOptionRepository.findByMemberIdAndVoteId(currentUserId, voteId);
            if (userVote.isPresent()) {
                hasVoted = true;
                votedOptionLabel = userVote.get().getVoteOption().getLabel().name();
            }
        }
        // --- 추가된 로직 끝 ---

        return VoteDetailResponse.builder()
                .voteId(vote.getId())
                .title(vote.getTitle())
                .category(vote.getCategory())
                .totalVoteCount(vote.getTotalVoteCount())
                .creatorNickname(creatorNickname) // 수정된 닉네임 사용
                .createdAt(vote.getCreatedAt())
                .options(optionDtos)
                .hasVoted(hasVoted) // 새로운 필드 값 설정
                .votedOptionLabel(votedOptionLabel) // 새로운 필드 값 설정
                .build();
    }




    @Override
    public Long createVote(Long userId, VoteCreateRequest request) {
        // 1. 회원 검증
        Member member = memberRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException("회원이 존재하지 않습니다.", HttpStatus.NOT_FOUND));


        // **수정된 부분: 제목 길이 검증 시 .trim() 추가**
        String trimmedTitle = request.getTitle() != null ? request.getTitle().trim() : null;
        if (trimmedTitle == null || trimmedTitle.isEmpty() || trimmedTitle.length() > 25) {
            throw new ApiException("투표 제목은 1자 이상 25자 이하여야 합니다 (공백 제외).", HttpStatus.BAD_REQUEST);
        }

        // 2. 투표 생성 (아직 데이터베이스에 저장되지 않은 비영속 상태)
        Vote vote = Vote.builder()
                .title(request.getTitle())
                .category(request.getCategory())
                .member(member)
                .build();

        // 3. 투표 옵션 생성 및 추가 (최대 4개 옵션 제한)
        List<String> options = request.getOptions();
        if (options == null || options.isEmpty() || options.size() > 4) {
            throw new ApiException("투표 옵션은 1개 이상 4개 이하여야 합니다.", HttpStatus.BAD_REQUEST);
        }

        VoteLabel[] labels = VoteLabel.values();
        for (int i = 0; i < options.size(); i++) {
            VoteOption voteOption = VoteOption.builder()
                    .content(options.get(i))
                    .label(labels[i])
                    .build();
            vote.addVoteOption(voteOption); // Vote 엔티티에 옵션 추가
        }

        // **수정된 부분: Vote를 먼저 저장하여 ID를 할당받은 영속 상태의 객체를 얻음**
        Vote savedVote = voteRepository.save(vote); // 투표를 저장하고, ID가 할당된 영속 객체를 반환받음

        // 4. CommentGroup 생성 (Vote와 1:1 관계)
        // CommentGroup을 빌드할 때, ID가 할당된 'savedVote' 객체를 사용
        CommentGroup commentGroup = CommentGroup.builder()
                .vote(savedVote) // 이제 'vote' 대신 'savedVote'를 사용하여 유효한 ID를 가진 Vote와 연결
                .totalCommentCount(0)
                .build();

        commentGroupRepository.save(commentGroup); // CommentGroup 저장

        return savedVote.getId(); // 저장된 투표의 ID를 반환
    }

    @Override
    public VoteListResponse getVotesByCategoryAndSort(String category, String sort, String cursor, int size) {
        List<Vote> votes = voteRepository.findVotesByCursor(category, sort, cursor, size);

        boolean hasNext = votes.size() > size;
        String nextCursor = null;

        if (hasNext) {
            votes.remove(votes.size() - 1);
            Vote lastVote = votes.get(votes.size() - 1);
            if ("popular".equalsIgnoreCase(sort)) {
                nextCursor = lastVote.getTotalVoteCount() + "_" + lastVote.getCreatedAt().toString();
            } else { // latest
                nextCursor = lastVote.getCreatedAt().toString();
            }
        }

        List<VoteListResponse.VoteDto> voteDtos = votes.stream()
                .map(vote -> {
                    String creatorNickname = "익명";
                    if (vote.getMember() != null && vote.getMember().getProfile() != null) {
                        creatorNickname = vote.getMember().getProfile().getNickname();
                    } else if (vote.getMember() != null && vote.getMember().getName() != null) {
                        creatorNickname = vote.getMember().getName();
                    }

                    List<VoteListResponse.VoteOptionListDto> optionListDtos = vote.getVoteOptions().stream()
                            .map(option -> VoteListResponse.VoteOptionListDto.builder()
                                    .id(option.getId())
                                    .content(option.getContent())
                                    .build())
                            .collect(Collectors.toList());

                    Integer totalCommentCount = 0;
                    if (vote.getCommentGroup() != null) {
                        totalCommentCount = vote.getCommentGroup().getTotalCommentCount();
                    }

                    return VoteListResponse.VoteDto.builder()
                            .id(vote.getId())
                            .title(vote.getTitle())
                            .category(vote.getCategory().name())
                            .member_id(vote.getMember() != null ? vote.getMember().getId() : null)
                            .nickname(creatorNickname)
                            .created_at(vote.getCreatedAt())
                            .total_vote_count(vote.getTotalVoteCount())
                            .total_comment_count(totalCommentCount)
                            .options(optionListDtos)
                            .build();
                })
                .collect(Collectors.toList());

        return VoteListResponse.builder()
                .votes(voteDtos)
                .has_next_page(hasNext)
                .next_cursor(nextCursor)
                .build();
    }
}
