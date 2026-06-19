package com.valanse.valanse.service.VoteService;

import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.enums.PinType;
import com.valanse.valanse.dto.Vote.*;
import com.valanse.valanse.domain.enums.VoteCategory;
import com.valanse.valanse.dto.Vote.VoteResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * VoteService 기능의 비즈니스 계약을 정의하는 서비스 인터페이스 코드입니다.
 */
public interface VoteService {

    // 신규 추가: 인기 급상승 토픽 조회
    HotIssueVoteResponse getTrendingVote();

    HotIssueVoteResponse getHotIssueVote();
    // 사용자의 투표를 처리하는 핵심 메서드.
    // userId: 현재 로그인한 사용자의 ID
    // voteId: 사용자가 투표하려는 투표의 ID
    // voteOptionId: 사용자가 선택한 투표 옵션의 ID
    VoteCancleResponseDto processVote(Long userId, Long voteId, Long voteOptionId);

    VoteDetailResponse getVoteDetailById(Long voteId);
    List<VoteResponseDto> getMyCreatedVotes(Long memberId, String sort, VoteCategory category);
    Long createVote(Long userId, VoteCreateRequest request);
    Long createVote(Long userId, VoteCreateRequest request, Map<String, MultipartFile> optionImageFiles);
    List<VoteResponseDto> getMyVotedVotes(Long memberId, String sort, VoteCategory category);
    // /votes get 메서드 cursor기반으로 변경
    VoteListResponse getVotesByCategoryAndSort(Member loginUser, String category, String sort, String cursor, int size);
    void deleteVote(Long userId, Long voteId);
    // 관리자의 고정 기능
    void updatePinStatus(Member member, Long voteId, PinType pinType);
}
