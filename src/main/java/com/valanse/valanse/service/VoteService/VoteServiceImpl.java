package com.valanse.valanse.service.VoteService;

import com.valanse.valanse.common.api.PaginationValidator;
import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.common.auth.SecurityUtils;
import com.valanse.valanse.common.message.MemberErrorMessage;
import com.valanse.valanse.common.message.VoteErrorMessage;
import com.valanse.valanse.domain.*;
import com.valanse.valanse.domain.enums.PinType;
import com.valanse.valanse.domain.enums.PointType;
import com.valanse.valanse.domain.enums.Role;
import com.valanse.valanse.domain.enums.VoteCategory;
import com.valanse.valanse.domain.enums.VoteLabel;
import com.valanse.valanse.domain.mapping.MemberVoteOption;
import com.valanse.valanse.dto.Vote.*;
import com.valanse.valanse.repository.*;
import com.valanse.valanse.service.PointService.PointService;
import com.valanse.valanse.service.StorageService.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime; // 기존 코드에 있었으므로 유지
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List; // 기존 코드에 있었으므로 유지
import java.util.Map;
import java.util.Optional; // Optional 임포트 추가 (processVote 메서드에서 사용)
import java.util.Set;
import java.util.stream.Collectors; // 기존 코드에 있었으므로 유지

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 클래스 레벨에서 기본적으로 읽기 전용 트랜잭션으로 설정
/**
 * 투표 생성, 참여/취소, 목록 조회, 핫이슈/트렌딩 선정, 삭제/고정 정책을 처리하는 서비스 코드입니다.
 */
public class VoteServiceImpl implements VoteService {

    private final VoteRepository voteRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final MemberRepository memberRepository;
    private final VoteOptionRepository voteOptionRepository;
    private final MemberVoteOptionRepository memberVoteOptionRepository;
    private final CommentGroupRepository commentGroupRepository;
    private final MemberProfileTitleRepository memberProfileTitleRepository;
    private final PointService pointService;
    private final StorageService storageService;
    private final Clock clock;

    private static final int TRENDING_LIMIT = 5;
    private static final int MIN_TRENDING_DAYS = 1;
    private static final int MAX_TRENDING_DAYS = 30;

   //작은 민지가 구현한 것
   /**
    * 사용자가 직접 생성한 투표 목록을 정렬과 카테고리 조건으로 조회하는 메서드입니다.
    */
   @Override
   public PagedVoteResponse getMyCreatedVotes(Long memberId, String sort, VoteCategory category, Pageable pageable) {
       Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
               .orElseThrow(() -> new ApiException(MemberErrorMessage.MEMBER_NOT_FOUND.message(), HttpStatus.NOT_FOUND));

       Slice<Vote> votes;

       boolean isAllCategory = category == VoteCategory.ALL;

       if (isAllCategory) {
           votes = sort.equals("latest") ?
                   voteRepository.findSliceByMemberOrderByCreatedAtDesc(member, pageable) :
                   voteRepository.findSliceByMemberOrderByCreatedAtAsc(member, pageable);
       } else {
           if (category == null)
               throw new ApiException(VoteErrorMessage.CATEGORY_REQUIRED.message(), HttpStatus.BAD_REQUEST);

           votes = sort.equals("latest") ?
                   voteRepository.findSliceByMemberAndCategoryOrderByCreatedAtDesc(member, category, pageable) :
                   voteRepository.findSliceByMemberAndCategoryOrderByCreatedAtAsc(member, category, pageable);
       }

       return toPagedVoteResponse(votes, pageable);
   }

    /**
     * 사용자가 참여한 투표 목록을 정렬과 카테고리 조건으로 조회하는 메서드입니다.
     */
    @Override
    public PagedVoteResponse getMyVotedVotes(Long memberId, String sort, VoteCategory category, Pageable pageable) {
        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new ApiException(MemberErrorMessage.MEMBER_NOT_FOUND.message(), HttpStatus.NOT_FOUND));

        Slice<Vote> votes;

        boolean isAllCategory = category == VoteCategory.ALL;

        if (isAllCategory) {
            votes = sort.equals("latest") ?
                    voteRepository.findSliceByMemberVotedOrderByCreatedAtDesc(member, pageable) :
                    voteRepository.findSliceByMemberVotedOrderByCreatedAtAsc(member, pageable);
        } else {
            if (category == null)
                throw new ApiException(VoteErrorMessage.CATEGORY_REQUIRED.message(), HttpStatus.BAD_REQUEST);

            votes = sort.equals("latest") ?
                    voteRepository.findSliceByMemberVotedAndCategoryOrderByCreatedAtDesc(member, category, pageable) :
                    voteRepository.findSliceByMemberVotedAndCategoryOrderByCreatedAtAsc(member, category, pageable);
        }

        return toPagedVoteResponse(votes, pageable);
    }

    private PagedVoteResponse toPagedVoteResponse(Slice<Vote> votes, Pageable pageable) {
        List<Long> creatorMemberIds = votes.getContent().stream()
                .map(Vote::getMember)
                .filter(member -> member != null && member.getId() != null)
                .map(Member::getId)
                .distinct()
                .toList();
        Map<Long, String> titleNamesByMemberId = getEquippedTitleNamesByMemberIds(creatorMemberIds);

        return PagedVoteResponse.builder()
                .votes(votes.getContent().stream()
                        .map(vote -> new VoteResponseDto(
                                vote,
                                vote.getMember() == null ? null : titleNamesByMemberId.get(vote.getMember().getId())
                        ))
                        .collect(Collectors.toList()))
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .hasNext(votes.hasNext())
                .build();
    }

    @Override
    public TrendingVotesResponse getTrendingVotes(int days) {
        validateTrendingDays(days);

        LocalDateTime to = LocalDateTime.now(clock);
        LocalDateTime from = to.minusDays(days);
        List<TrendingVoteScore> periodScores = voteRepository
                .findTopTrendingVotes(from, to, TRENDING_LIMIT);

        boolean fallbackApplied = periodScores.isEmpty();
        TrendingScoreType scoreType = fallbackApplied
                ? TrendingScoreType.ALL_TIME
                : TrendingScoreType.PERIOD;
        List<TrendingVoteScore> rankedScores = fallbackApplied
                ? voteRepository.findTopAllTimeTrendingVotes(TRENDING_LIMIT)
                : periodScores;

        Optional<Vote> pinnedVote = voteRepository.findByPinType(PinType.TRENDING);
        List<SelectedTrendingVote> selectedVotes = selectTrendingVotes(
                rankedScores,
                pinnedVote,
                scoreType,
                from,
                to
        );

        List<TrendingVoteItemResponse> voteItems = buildTrendingVoteItems(selectedVotes);
        return TrendingVotesResponse.builder()
                .requestedDays(days)
                .from(from)
                .to(to)
                .scoreType(scoreType)
                .fallbackApplied(fallbackApplied)
                .votes(voteItems)
                .build();
    }

    private void validateTrendingDays(int days) {
        if (days < MIN_TRENDING_DAYS || days > MAX_TRENDING_DAYS) {
            throw new ApiException(VoteErrorMessage.TRENDING_PERIOD_INVALID.message(), HttpStatus.BAD_REQUEST);
        }
    }

    private List<SelectedTrendingVote> selectTrendingVotes(
            List<TrendingVoteScore> rankedScores,
            Optional<Vote> pinnedVote,
            TrendingScoreType scoreType,
            LocalDateTime from,
            LocalDateTime to
    ) {
        List<SelectedTrendingVote> selectedVotes = new ArrayList<>();
        Long pinnedVoteId = pinnedVote.map(Vote::getId).orElse(null);

        if (pinnedVoteId != null) {
            TrendingVoteScore pinnedScore = rankedScores.stream()
                    .filter(score -> score.voteId().equals(pinnedVoteId))
                    .findFirst()
                    .orElseGet(() -> findPinnedVoteScore(pinnedVoteId, scoreType, from, to));
            selectedVotes.add(new SelectedTrendingVote(
                    pinnedVoteId,
                    pinnedScore,
                    TrendingDisplayType.PINNED
            ));
        }

        rankedScores.stream()
                .filter(score -> !score.voteId().equals(pinnedVoteId))
                .limit(TRENDING_LIMIT - selectedVotes.size())
                .map(score -> new SelectedTrendingVote(
                        score.voteId(),
                        score,
                        TrendingDisplayType.RANKED
                ))
                .forEach(selectedVotes::add);

        return selectedVotes;
    }

    private TrendingVoteScore findPinnedVoteScore(
            Long pinnedVoteId,
            TrendingScoreType scoreType,
            LocalDateTime from,
            LocalDateTime to
    ) {
        Optional<TrendingVoteScore> score = scoreType == TrendingScoreType.PERIOD
                ? voteRepository.findTrendingScoreByVoteId(pinnedVoteId, from, to)
                : voteRepository.findAllTimeTrendingScoreByVoteId(pinnedVoteId);
        return score.orElseGet(() -> TrendingVoteScore.empty(pinnedVoteId));
    }

    private List<TrendingVoteItemResponse> buildTrendingVoteItems(
            List<SelectedTrendingVote> selectedVotes
    ) {
        if (selectedVotes.isEmpty()) {
            return List.of();
        }

        List<Long> voteIds = selectedVotes.stream()
                .map(SelectedTrendingVote::voteId)
                .toList();
        Map<Long, Vote> votesById = voteRepository.findTrendingVoteDetailsByIds(voteIds).stream()
                .collect(Collectors.toMap(Vote::getId, vote -> vote));

        List<Long> memberIds = votesById.values().stream()
                .map(Vote::getMember)
                .filter(member -> member != null && member.getId() != null)
                .map(Member::getId)
                .distinct()
                .toList();
        Map<Long, String> titleNamesByMemberId = getEquippedTitleNamesByMemberIds(memberIds);
        Map<Long, String> nicknamesByMemberId = getNicknamesByMemberIds(memberIds);

        List<TrendingVoteItemResponse> responses = new ArrayList<>();
        for (SelectedTrendingVote selectedVote : selectedVotes) {
            Vote vote = votesById.get(selectedVote.voteId());
            if (vote == null) {
                continue;
            }
            responses.add(toTrendingVoteItemResponse(
                    responses.size() + 1,
                    selectedVote,
                    vote,
                    titleNamesByMemberId,
                    nicknamesByMemberId
            ));
        }
        return responses;
    }

    private TrendingVoteItemResponse toTrendingVoteItemResponse(
            int displayOrder,
            SelectedTrendingVote selectedVote,
            Vote vote,
            Map<Long, String> titleNamesByMemberId,
            Map<Long, String> nicknamesByMemberId
    ) {
        TrendingVoteScore score = selectedVote.score();
        Member creator = vote.getMember();
        String createdBy = "익명";
        String creatorTitle = null;
        if (creator != null) {
            if (nicknamesByMemberId.get(creator.getId()) != null) {
                createdBy = nicknamesByMemberId.get(creator.getId());
            } else if (creator.getName() != null) {
                createdBy = creator.getName();
            }
            creatorTitle = titleNamesByMemberId.get(creator.getId());
        }

        List<HotIssueVoteOptionDto> options = vote.getVoteOptions().stream()
                .map(option -> HotIssueVoteOptionDto.builder()
                        .optionId(option.getId())
                        .content(option.getContent())
                        .imageUrl(option.getImageUrl())
                        .vote_count(option.getVoteCount())
                        .build())
                .toList();

        return TrendingVoteItemResponse.builder()
                .displayOrder(displayOrder)
                .displayType(selectedVote.displayType())
                .voteId(vote.getId())
                .title(vote.getTitle())
                .content(vote.getContent())
                .category(vote.getCategory() == null ? null : vote.getCategory().name())
                .reactivityScore(score.reactivityScore())
                .voteReactionCount(score.voteReactionCount())
                .commentReactionCount(score.commentReactionCount())
                .totalParticipants(vote.getTotalVoteCount())
                .createdBy(createdBy)
                .creatorTitle(creatorTitle)
                .createdAt(vote.getCreatedAt())
                .options(options)
                .build();
    }

    private Map<Long, String> getNicknamesByMemberIds(List<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return Map.of();
        }

        return memberProfileRepository.findAllByMemberIdIn(memberIds).stream()
                .filter(profile -> profile.getMember() != null)
                .filter(profile -> profile.getNickname() != null)
                .collect(Collectors.toMap(
                        profile -> profile.getMember().getId(),
                        MemberProfile::getNickname,
                        (existing, replacement) -> existing
                ));
    }

    private record SelectedTrendingVote(
            Long voteId,
            TrendingVoteScore score,
            TrendingDisplayType displayType
    ) {
    }

    /**
     * 사용자의 투표 선택, 취소, 재선택을 처리하고 선택지별 카운트와 전체 카운트를 갱신하는 메서드입니다.
     */
    @Override
    @Transactional // 이 메서드는 데이터를 변경하므로 읽기/쓰기 트랜잭션이 필요합니다. (클래스 레벨의 readOnly = true를 오버라이드)
    public VoteCancleResponseDto processVote(Long userId, Long voteId, Long voteOptionId) {
        // 1. 필수 엔티티들을 조회합니다. (없으면 예외 발생)
        Member member = memberRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException(MemberErrorMessage.MEMBER_NOT_FOUND.message(), HttpStatus.NOT_FOUND));

        Vote vote = findVoteForUpdate(voteId)
                .orElseThrow(() -> new ApiException(VoteErrorMessage.VOTE_NOT_FOUND.message(), HttpStatus.NOT_FOUND));

        VoteOption newVoteOption = findVoteOptionForUpdate(voteOptionId)
                .orElseThrow(() -> new ApiException(VoteErrorMessage.VOTE_OPTION_NOT_FOUND.message(), HttpStatus.NOT_FOUND));

        if (newVoteOption.getVote() == null || !voteId.equals(newVoteOption.getVote().getId())) {
            throw new ApiException(VoteErrorMessage.VOTE_OPTION_NOT_BELONG_TO_VOTE.message(), HttpStatus.BAD_REQUEST);
        }

        // 2. 사용자가 이 투표에 대해 이전에 투표한 선택지가 있는지 확인합니다.
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
                // member_vote_option에서 해당 기록을 삭제
                memberVoteOptionRepository.delete(oldMemberVoteOption);
                // 기존 선택지의 투표 수 감소
                oldVoteOption.setVoteCount(decrementCount(oldVoteOption.getVoteCount()));
                // 전체 투표 수 감소
                vote.setTotalVoteCount(decrementCount(vote.getTotalVoteCount()));

                isVoted = false; // 투표가 취소되었으므로 false
                updatedTotalVoteCount = vote.getTotalVoteCount();
                updatedVoteOptionCount = oldVoteOption.getVoteCount();

            } else {
                // 3-2. 다른 선택지를 클릭한 경우: 기존 투표 기록의 선택지만 변경 (재선택)
                // 같은 (member_id, vote_id)로 delete 후 insert하면 Hibernate의 실행 순서에 따라
                // unique 제약조건 위반이 발생할 수 있으므로 기존 행을 update합니다.
                oldMemberVoteOption.changeVoteOption(newVoteOption);
                // 기존 선택지의 투표 수 감소
                oldVoteOption.setVoteCount(decrementCount(oldVoteOption.getVoteCount()));
                // 새로운 선택지의 투표 수 증가
                newVoteOption.setVoteCount(incrementCount(newVoteOption.getVoteCount()));

                // 전체 투표수는 변동 없음 (기존 1 감소, 새로운 1 증가)
                isVoted = true; // 새로운 옵션에 투표했으므로 true
                updatedTotalVoteCount = vote.getTotalVoteCount();
                updatedVoteOptionCount = newVoteOption.getVoteCount();
            }
        } else {
            // 4. 이전에 투표한 기록이 없는 경우: 새로운 투표 기록
            MemberVoteOption newMemberVoteOption = MemberVoteOption.builder()
                    .member(member)
                    .vote(vote)
                    .voteOption(newVoteOption)
                    .build();
            saveMemberVoteOption(newMemberVoteOption);
            newVoteOption.setVoteCount(incrementCount(newVoteOption.getVoteCount()));
            vote.setTotalVoteCount(incrementCount(vote.getTotalVoteCount()));

            // 게시물 작성자에게 투표 참여 포인트 지급
            if (vote.getMember() != null && !vote.getMember().getId().equals(userId)) {
                pointService.givePoint(vote.getMember().getId(), PointType.POST_VOTED);
            }

            isVoted = true;
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

    private Optional<Vote> findVoteForUpdate(Long voteId) {
        Optional<Vote> lockedVote = voteRepository.findByIdForUpdate(voteId);
        if (lockedVote != null && lockedVote.isPresent()) {
            return lockedVote;
        }
        return voteRepository.findById(voteId);
    }

    private Optional<VoteOption> findVoteOptionForUpdate(Long voteOptionId) {
        Optional<VoteOption> lockedVoteOption = voteOptionRepository.findByIdForUpdate(voteOptionId);
        if (lockedVoteOption != null && lockedVoteOption.isPresent()) {
            return lockedVoteOption;
        }
        return voteOptionRepository.findById(voteOptionId);
    }

    private void saveMemberVoteOption(MemberVoteOption memberVoteOption) {
        try {
            memberVoteOptionRepository.save(memberVoteOption);
            memberVoteOptionRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(VoteErrorMessage.VOTE_ALREADY_PROCESSED.message(), HttpStatus.BAD_REQUEST);
        }
    }

    private int incrementCount(Integer count) {
        return (count == null ? 0 : count) + 1;
    }

    private int decrementCount(Integer count) {
        return Math.max(0, (count == null ? 0 : count) - 1);
    }
    /**
     * 투표 상세 정보와 현재 사용자의 투표 여부를 함께 조회하는 메서드입니다.
     */
    @Override
    public VoteDetailResponse getVoteDetailById(Long voteId) {
        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new ApiException(VoteErrorMessage.VOTE_DETAIL_NOT_FOUND.message(), HttpStatus.NOT_FOUND));

        // MemberProfile의 nickname을 가져오도록 수정
        String creatorNickname = "탈퇴한 사용자";
        String creatorTitle = getEquippedTitleName(vote.getMember());
        if (vote.getMember() != null && vote.getMember().getProfile() != null) {
            creatorNickname = vote.getMember().getProfile().getNickname();
        }

        List<VoteDetailResponse.VoteOptionDto> optionDtos = vote.getVoteOptions().stream()
                .map(option -> VoteDetailResponse.VoteOptionDto.builder()
                        .optionId(option.getId())
                        .content(option.getContent())
                        .imageUrl(option.getImageUrl())
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
            }
        } catch (NumberFormatException e) {
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
                .content(vote.getContent())
                .category(vote.getCategory())
                .totalVoteCount(vote.getTotalVoteCount())
                .creatorNickname(creatorNickname) // 수정된 닉네임 사용
                .creatorTitle(creatorTitle)
                .createdAt(vote.getCreatedAt())
                .options(optionDtos)
                .hasVoted(hasVoted) // 새로운 필드 값 설정
                .votedOptionLabel(votedOptionLabel) // 새로운 필드 값 설정
                .build();
    }




    /**
     * 새 투표와 선택지, 댓글 그룹을 생성하고 작성 포인트를 지급하는 메서드입니다.
     */
    @Override
    @Transactional
    public Long createVote(Long userId, VoteCreateRequest request) {
        return createVote(userId, request, Map.of());
    }

    @Override
    @Transactional
    public Long createVote(Long userId, VoteCreateRequest request, Map<String, MultipartFile> optionImageFiles) {
        // 1. 회원 검증
        Member member = memberRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException(MemberErrorMessage.MEMBER_NOT_FOUND.message(), HttpStatus.NOT_FOUND));


        // **수정된 부분: 제목 길이 검증 시 .trim() 추가**
        String trimmedTitle = request.getTitle() != null ? request.getTitle().trim() : null;
        if (trimmedTitle == null || trimmedTitle.isEmpty() || trimmedTitle.length() > 25) {
            throw new ApiException(VoteErrorMessage.VOTE_TITLE_INVALID.message(), HttpStatus.BAD_REQUEST);
        }

        // 2. 투표 생성 (아직 데이터베이스에 저장되지 않은 비영속 상태)
        Vote vote = Vote.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .category(request.getCategory())
                .member(member)
                .pinType(PinType.NONE)
                .build();

        // 3. 투표 옵션 생성 및 추가 (최대 4개 옵션 제한)
        List<VoteCreateRequest.OptionRequest> options = request.getOptions();
        if (options == null || options.isEmpty() || options.size() > 4) {
            throw new ApiException(VoteErrorMessage.VOTE_OPTION_COUNT_INVALID.message(), HttpStatus.BAD_REQUEST);
        }

        validateOptionImageKeys(options);

        VoteLabel[] labels = VoteLabel.values();
        for (int i = 0; i < options.size(); i++) {
            VoteCreateRequest.OptionRequest option = options.get(i);
            String imageUrl = uploadOptionImage(option, optionImageFiles);
            VoteOption voteOption = VoteOption.builder()
                    .content(option.getContent())
                    .imageUrl(imageUrl)
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

        // 게시물 작성 포인트 지급
        pointService.givePoint(userId, PointType.POST_CREATE);

        return savedVote.getId(); // 저장된 투표의 ID를 반환
    }

    private void validateOptionImageKeys(List<VoteCreateRequest.OptionRequest> options) {
        Set<String> imageKeys = new HashSet<>();
        for (VoteCreateRequest.OptionRequest option : options) {
            String imageKey = option.getImageKey();
            if (!StringUtils.hasText(imageKey)) {
                continue;
            }
            if (!imageKeys.add(imageKey)) {
                throw new ApiException(VoteErrorMessage.VOTE_OPTION_IMAGE_KEY_DUPLICATED.message(), HttpStatus.BAD_REQUEST);
            }
        }
    }

    private String uploadOptionImage(VoteCreateRequest.OptionRequest option, Map<String, MultipartFile> optionImageFiles) {
        String imageKey = option.getImageKey();
        if (!StringUtils.hasText(imageKey)) {
            return null;
        }

        MultipartFile imageFile = optionImageFiles.get(imageKey);
        if (imageFile == null || imageFile.isEmpty()) {
            throw new ApiException(VoteErrorMessage.VOTE_OPTION_IMAGE_NOT_FOUND.message(), HttpStatus.BAD_REQUEST);
        }

        return storageService.uploadImage(imageFile, "vote-options");
    }

    /**
     * 커서 기반 페이지네이션으로 투표 목록을 조회하고 목록 응답 DTO를 구성하는 메서드입니다.
     */
    @Override
    public VoteListResponse getVotesByCategoryAndSort(Member loginUser, String category, String sort, String cursor, int size) {
        validateVoteListRequest(category, sort, cursor, size);

        List<Vote> votes = voteRepository.findVotesByCursor(category, sort, cursor, size);

        boolean hasNext = votes.size() > size;
        String nextCursor = null;

        if (hasNext) {
            votes.remove(votes.size() - 1);
            Vote lastVote = votes.get(votes.size() - 1);
            if ("popular".equalsIgnoreCase(sort)) {
                nextCursor = lastVote.getTotalVoteCount() + "_" + lastVote.getCreatedAt() + "_" + lastVote.getId();
            } else { // latest
                nextCursor = lastVote.getCreatedAt() + "_" + lastVote.getId();
            }
        }

        List<Long> creatorMemberIds = votes.stream()
                .map(Vote::getMember)
                .filter(member -> member != null && member.getId() != null)
                .map(Member::getId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> equippedTitleNamesByMemberId = getEquippedTitleNamesByMemberIds(creatorMemberIds);

        List<VoteListResponse.VoteDto> voteDtos = votes.stream()
                .map(vote -> {
                    String creatorNickname = "탈퇴한 사용자";
                    if (vote.getMember() != null && vote.getMember().getProfile() != null) {
                        creatorNickname = vote.getMember().getProfile().getNickname();
                    } else if (vote.getMember() != null && vote.getMember().getName() != null) {
                        creatorNickname = vote.getMember().getName();
                    }

                    List<VoteListResponse.VoteOptionListDto> optionListDtos = vote.getVoteOptions().stream()
                            .map(option -> VoteListResponse.VoteOptionListDto.builder()
                                    .id(option.getId())
                                    .content(option.getContent())
                                    .imageUrl(option.getImageUrl())
                                    .build())
                            .collect(Collectors.toList());

                    Integer totalCommentCount = 0;
                    if (vote.getCommentGroup() != null) {
                        totalCommentCount = vote.getCommentGroup().getTotalCommentCount();
                    }

                    boolean isAdmin = loginUser != null && loginUser.getRole() == Role.ADMIN;
                    boolean canDelete = false;
                    if (loginUser != null && vote.getMember() != null) {
                        canDelete = isAdmin || vote.getMember().getId().equals(loginUser.getId());
                    }
                    return VoteListResponse.VoteDto.builder()
                            .id(vote.getId())
                            .title(vote.getTitle())
                            .content(vote.getContent())
                            .category(vote.getCategory().name())
                            .member_id(vote.getMember() != null ? vote.getMember().getId() : null)
                            .nickname(creatorNickname)
                            .member_title(vote.getMember() != null
                                    ? equippedTitleNamesByMemberId.get(vote.getMember().getId())
                                    : null)
                            .created_at(vote.getCreatedAt())
                            .total_vote_count(vote.getTotalVoteCount())
                            .total_comment_count(totalCommentCount)
                            .options(optionListDtos)
                            .canDelete(canDelete)
                            .build();
                })
                .collect(Collectors.toList());

        return VoteListResponse.builder()
                .votes(voteDtos)
                .has_next_page(hasNext)
                .next_cursor(nextCursor)
                .build();
    }

    private void validateVoteListRequest(String category, String sort, String cursor, int size) {
        PaginationValidator.validateSize(size);

        validateCategory(category);
        validateSort(sort);
        validateCursor(sort, cursor);
    }

    private void validateCategory(String category) {
        if (category == null || category.isBlank()) {
            throw new ApiException(VoteErrorMessage.CATEGORY_INVALID.message(), HttpStatus.BAD_REQUEST);
        }

        boolean validCategory = Arrays.stream(VoteCategory.values())
                .anyMatch(voteCategory -> voteCategory.name().equalsIgnoreCase(category));
        if (!validCategory) {
            throw new ApiException(VoteErrorMessage.CATEGORY_INVALID.message(), HttpStatus.BAD_REQUEST);
        }
    }

    private void validateSort(String sort) {
        if (sort == null || sort.isBlank()) {
            throw new ApiException(VoteErrorMessage.SORT_INVALID.message(), HttpStatus.BAD_REQUEST);
        }

        if (!"latest".equalsIgnoreCase(sort) && !"popular".equalsIgnoreCase(sort)) {
            throw new ApiException(VoteErrorMessage.SORT_INVALID.message(), HttpStatus.BAD_REQUEST);
        }
    }

    private void validateCursor(String sort, String cursor) {
        if (cursor == null) {
            return;
        }

        if (cursor.isBlank()) {
            throw new ApiException(VoteErrorMessage.CURSOR_INVALID.message(), HttpStatus.BAD_REQUEST);
        }

        try {
            if ("popular".equalsIgnoreCase(sort)) {
                String[] parts = cursor.split("_");
                if (parts.length != 3) {
                    throw new ApiException(VoteErrorMessage.CURSOR_INVALID.message(), HttpStatus.BAD_REQUEST);
                }
                Integer.parseInt(parts[0]);
                LocalDateTime.parse(parts[1]);
                Long.parseLong(parts[2]);
            } else {
                String[] parts = cursor.split("_");
                if (parts.length != 2) {
                    throw new ApiException(VoteErrorMessage.CURSOR_INVALID.message(), HttpStatus.BAD_REQUEST);
                }
                LocalDateTime.parse(parts[0]);
                Long.parseLong(parts[1]);
            }
        } catch (RuntimeException e) {
            throw new ApiException(VoteErrorMessage.CURSOR_INVALID.message(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * 투표 작성자 또는 관리자가 투표를 소프트 삭제하는 메서드입니다.
     */
    @Override
    @Transactional
    public void deleteVote(Long userId, Long voteId) {
        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new ApiException(VoteErrorMessage.VOTE_DETAIL_NOT_FOUND.message(), HttpStatus.NOT_FOUND));

        boolean isAdminToken = userId != null && userId == 0L && SecurityUtils.isCurrentUserAdmin();
        Member member = isAdminToken
                ? Member.builder().id(userId).role(Role.ADMIN).build()
                : memberRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException(MemberErrorMessage.MEMBER_NOT_FOUND.message(), HttpStatus.NOT_FOUND));

        // 권한 확인 - 자기 자신 혹은 관리자
        if ((vote.getMember() == null || !vote.getMember().getId().equals(userId)) && member.getRole() != Role.ADMIN) {
            throw new ApiException(VoteErrorMessage.DELETE_PERMISSION_DENIED.message(), HttpStatus.FORBIDDEN);
        }

        // BaseEntity의 softDelete() 메서드 사용
        vote.softDelete();
        voteRepository.save(vote);
    }

    @Override
    @Transactional
    // 고정 , 고정 해제 기능은 vote 엔티티에 메서드로 존재
    /**
     * 관리자 권한으로 투표의 트렌딩 고정 상태를 변경하는 메서드입니다.
     */
    public void updatePinStatus(Member member, Long voteId, PinType pinType) {
        if (member.getRole() != Role.ADMIN) {
            throw new ApiException(VoteErrorMessage.PERMISSION_DENIED.message(), HttpStatus.FORBIDDEN);
        }
        if (pinType == null) {
            throw new ApiException(VoteErrorMessage.PIN_TYPE_REQUIRED.message(), HttpStatus.BAD_REQUEST);
        }
        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new ApiException(VoteErrorMessage.POST_NOT_FOUND.message(), HttpStatus.NOT_FOUND));
        if (pinType != PinType.NONE) {
            // 이미 고정된 다른 게시물이 존재한다면 해당 게시물의 고정 해제
            voteRepository.findByPinType(pinType).ifPresent(existing ->
                    {if (!existing.getId().equals(voteId))
                    {
                        existing.unpin();
                        voteRepository.save(existing);
                    }});
            vote.pin(pinType);
        }
        else {
            vote.unpin();
        }
        voteRepository.save(vote);
    }

    private String getEquippedTitleName(Member member) {
        if (member == null || member.getId() == null) {
            return null;
        }

        return memberProfileTitleRepository.findByMemberProfileMemberIdAndEquippedTrue(member.getId())
                .map(MemberProfileTitle::getTitle)
                .map(Title::getName)
                .orElse(null);
    }

    private Map<Long, String> getEquippedTitleNamesByMemberIds(List<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return Map.of();
        }

        return memberProfileTitleRepository.findAllEquippedWithTitleByMemberIds(memberIds).stream()
                .filter(memberProfileTitle -> memberProfileTitle.getMemberProfile() != null)
                .filter(memberProfileTitle -> memberProfileTitle.getMemberProfile().getMember() != null)
                .filter(memberProfileTitle -> memberProfileTitle.getTitle() != null)
                .collect(Collectors.toMap(
                        memberProfileTitle -> memberProfileTitle.getMemberProfile().getMember().getId(),
                        memberProfileTitle -> memberProfileTitle.getTitle().getName(),
                        (existing, replacement) -> existing
                ));
    }

}
