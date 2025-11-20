// src/main/java/com/valanse/valanse/controller/VoteController.java
package com.valanse.valanse.controller;
import com.valanse.valanse.domain.enums.PinType;
import com.valanse.valanse.service.MemberService.MemberService;
import com.valanse.valanse.service.VoteService.VoteService;
import com.valanse.valanse.dto.Vote.*;
import com.valanse.valanse.dto.Vote.VoteResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.valanse.valanse.domain.enums.VoteCategory;

import java.util.List;

@Tag(name = "투표 API", description = "투표 관련 API")
@RestController
@RequestMapping("/votes") // 투표 관련 API의 기본 경로
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;
    private final MemberService memberService;

    @GetMapping("/mine/created")
    @Operation(
            summary = "내가 만든 밸런스게임 목록 가져오기",
            description = "내가 만든 밸런스게임 목록을 시간순(latest/oldest)으로 반환합니다."
    )
    public ResponseEntity<List<VoteResponseDto>> getMyCreatedVotes(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String category,       // 추가
            @RequestParam(defaultValue = "latest") String sort
    ) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        VoteCategory voteCategory = null;

        if (category != null && !category.isEmpty()) {
            voteCategory = convertCategory(category);
        }

        List<VoteResponseDto> votes = voteService.getMyCreatedVotes(memberId, sort, voteCategory);
        return ResponseEntity.ok(votes);
    }

    @GetMapping("/mine/voted")
    @Operation(
            summary = "내가 투표한 밸런스게임 목록 가져오기",
            description = "내가 투표한 밸런스게임 목록을 시간순(latest/oldest)으로 반환합니다."
    )
    public ResponseEntity<List<VoteResponseDto>> getMyVotedVotes(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String category,       // 추가
            @RequestParam(defaultValue = "latest") String sort
    ) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        VoteCategory voteCategory = null;

        if (category != null && !category.isEmpty()) {
            voteCategory = convertCategory(category);
        }

        List<VoteResponseDto> votes = voteService.getMyVotedVotes(memberId, sort, voteCategory);
        return ResponseEntity.ok(votes);
    }

    private VoteCategory convertCategory(String category) {
        try {
            return VoteCategory.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid category: " + category);
        }
    }

   //여기서 부터 영서 부분
    @Operation(
            summary = "오늘의 핫이슈 밸런스 게임 선택지들 반환",
            description = "가장 많이 참여한 밸런스 게임 투표의 상세 정보와 옵션 목록을 조회합니다. " +
                    "총 참여자 수가 가장 많은 투표를 반환하며, 참여자 수가 동일한 경우 가장 최근에 생성된 투표를 우선합니다. \n" +
                    "  voteId -> 가장 투표 참여 횟수가 많은 투표의 id\n" +
                    "  title -> 가장 투표 참여 횟수가 많은 투표의 제목\n" +
                    "  category -> 가장 투표 참여 횟수가 많은 투표의 카테고리\n" +
                    "  totalParticipants -> 가장 투표 참여 횟수가 많은 투표의 총 투표 수\n" +
                    "  createdBy -> 가장 투표 참여 횟수가 많은 투표를 생성한 사람의 닉네임\n" +
                    "  createdAt -> 투표 생성 날짜\n" +
                    "  options -> 투표 옵션 리스트 (content, vote_count)"
    )
    @GetMapping("/best") // 새로운 엔드포인트: /votes/best (GET 메서드)
    public ResponseEntity<HotIssueVoteResponse> getHotIssueVote() {
        HotIssueVoteResponse response = voteService.getHotIssueVote();
        return ResponseEntity.ok(response);
    }

    // 인기 급상승 토픽
    @Operation(
            summary = "인기 급상승 밸런스 게임 선택지들 반환",
            description = "최근 7일 이내 반응성(투표수 + 댓글수)이 가장 높은 밸런스 게임을 반환합니다. " +
                    "7일 이내 새로 추가되는 반응이 없을 경우 이전 데이터를 유지합니다.\n" +
                    "  voteId -> 7일 내 반응성이 가장 높은 투표의 id\n" +
                    "  title -> 7일 내 반응성이 가장 높은 투표의 제목\n" +
                    "  category -> 7일 내 반응성이 가장 높은 투표의 카테고리\n" +
                    "  totalParticipants -> 7일 내 반응성이 가장 높은 투표의 총 투표 수\n" +
                    "  createdBy -> 7일 내 반응성이 가장 높은 투표를 생성한 사람의 닉네임\n" +
                    "  createdAt -> 투표 생성 날짜\n" +
                    "  options -> 투표 옵션 리스트 (content, vote_count)"
    )
    @GetMapping("/trending")
    public ResponseEntity<HotIssueVoteResponse> getTrendingVote() {
        HotIssueVoteResponse response = voteService.getTrendingVote();
        return ResponseEntity.ok(response);
    }


    @Operation(
            summary = "투표 선택, 취소, 재선택",
            description = "사용자가 투표 선택지를 클릭했을 때 호출됩니다. URL 경로에 있는 {voteId}와 {voteOptionId}를 통해 어떤 투표의 어떤 선택지를 조작하는지 명확히 전달합니다. 이미 투표한 선택지를 다시 클릭하면 투표가 취소되고, 다른 선택지를 클릭하면 기존 투표를 취소하고 새로운 선택지에 투표합니다." +
                        "  isVoted -> 투표를 취소한 요청인지 투표를 추가한 요청인지 구분\n" +
                    " totalVoteCount -> POST 요청 후 업데이트 된 총 투표 수\n" +
                    " voteOptionId -> POST 요청 후 업데이트 된 선택지의 id\n" +
                    " voteOptionCount -> POST 요청 후 업데이트 된 선택지 별 투표 수"
    )
    @PostMapping("/{voteId}/vote-options/{voteOptionId}") // Path Variable 사용
    public ResponseEntity<VoteCancleResponseDto> processVote(
            @PathVariable("voteId") Long voteId, // URL 경로에서 voteId를 추출
            @PathVariable("voteOptionId") Long voteOptionId) { // URL 경로에서 voteOptionId를 추출
        // 현재 로그인한 사용자의 ID를 SecurityContextHolder에서 가져옵니다.
        // Spring Security를 통해 인증된 사용자의 ID는 String 형태로 저장되어 있으므로 Long으로 변환합니다.
        Long userId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());

        // 서비스 계층의 processVote 메서드를 호출하여 실제 비즈니스 로직을 수행합니다.
        VoteCancleResponseDto response = voteService.processVote(userId, voteId, voteOptionId);

        // 서비스의 처리 결과를 HTTP 200 OK 상태 코드와 함께 클라이언트에게 반환합니다.
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "밸런스 게임 클릭했을 때 화면 불러오기",
            description = "ID를 통해 특정 투표의 상세 정보(제목, 옵션, 카테고리 등)를 조회합니다. 카테고리는 ENUM타입으로 LOVE, FOOD, ETC 만 존재합니다."
    )
    @GetMapping("/{voteId}")
    public ResponseEntity<VoteDetailResponse> getVoteDetail(@PathVariable("voteId") Long voteId) {
        VoteDetailResponse response = voteService.getVoteDetailById(voteId);
        return ResponseEntity.ok(response);
    }


    @Operation(
            summary = "투표 생성",
            description = "새로운 투표와 해당 투표의 옵션을 생성합니다. 각 투표는 최대 4개의 옵션을 가질 수 있습니다. \n" +
                    " Category에는 ETC ,FOOD , LOVE 이 3가지만 올 수 있습니다. 내부에서 ENUM으로 처리됩니다."
    )
    @PostMapping
    public ResponseEntity<VoteCreateResponse> createVote(
            @RequestBody VoteCreateRequest request
    ) {
        Long userId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
        Long voteId = voteService.createVote(userId, request);
        return ResponseEntity.ok(new VoteCreateResponse(voteId));
    }

@Operation(
        summary = "카테고리별/정렬 방식별 투표 목록 조회",
        description = "카테고리와 정렬 기준에 따라 투표 목록을 조회합니다. 'category' 파라미터는 'ETC', 'FOOD', 'LOVE', 'ALL' 중 하나를 받을 수 있으며, 'sort' 파라미터는 'popular' (인기순) 또는 'latest' (최신순) 중 하나를 받습니다. 페이징을 지원합니다. \n" +
                " ALL은 대소문자 구분없이 String으로 내부에서 처리합니다. LOVE ,FOOD ,ETC는 enum 타입으로 내부에서 처리됩니다. 만약에 category와 sort 파라미터가 없다면 모두 기본값인 ALL 과 latest로 자동 처리됩니다.\n" +
                " 그러나 category = , sort= 와 같이 =뒤에 비어있는 경우는 불가능합니다. 아예 존재하지 않을 경우만 기본값으로 설정됩니다."
)
@GetMapping
public ResponseEntity<VoteListResponse> getVotes(
        @RequestParam(value = "category", required = false, defaultValue = "ALL") String category,
        @RequestParam(value = "sort", required = false, defaultValue = "latest") String sort,
        @RequestParam(value = "cursor", required = false) String cursor, // 변경된 파라미터
        @RequestParam(value = "size", defaultValue = "10") int size
) {
    // Pageable 객체 대신 cursor, size를 직접 전달
    Long loginId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
    var loginMember = memberService.findById(loginId);
    VoteListResponse response = voteService.getVotesByCategoryAndSort(loginMember, category, sort, cursor, size);
    return ResponseEntity.ok(response);
}

    // sort 문자열에 따라 실제 정렬 속성을 반환하는 헬퍼 메서드 (필요시 VoteServiceImpl에서 가져올 수 있음)
    private String getSortProperty(String sort) {
        if ("popular".equalsIgnoreCase(sort)) {
            return "totalVoteCount"; // 또는 "totalVoteCount,createdAt" 등으로 복합 정렬 지정
        }
        return "createdAt"; // 기본은 최신순
    }

    @DeleteMapping("/{voteId}")
    @Operation(summary = "투표 삭제", description = "내가 작성한 투표를 삭제합니다.")
    public ResponseEntity<Void> deleteVote(@PathVariable("voteId") Long voteId) {
        Long userId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
        voteService.deleteVote(userId, voteId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{voteId}/pin")
    @Operation(summary = "고정", description = "관리자 권한으로 게시물을 고정합니다.")
    public ResponseEntity<Void> updatePinStatus(
            @PathVariable Long voteId,
            @RequestParam PinType pinType)
    {
        Long loginId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
        var member = memberService.findById(loginId);
        voteService.updatePinStatus(member, voteId, pinType);
        return ResponseEntity.ok().build();
    }

}
