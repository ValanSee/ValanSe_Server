package com.valanse.valanse.service.VoteService;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.domain.CommentGroup;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.Vote;
import com.valanse.valanse.domain.VoteOption;
import com.valanse.valanse.domain.enums.PinType;
import com.valanse.valanse.domain.enums.Role;
import com.valanse.valanse.domain.enums.VoteCategory;
import com.valanse.valanse.domain.enums.VoteLabel;
import com.valanse.valanse.domain.mapping.MemberVoteOption;
import com.valanse.valanse.dto.Vote.VoteCancleResponseDto;
import com.valanse.valanse.dto.Vote.VoteCreateRequest;
import com.valanse.valanse.dto.Vote.VoteDetailResponse;
import com.valanse.valanse.repository.*;
import com.valanse.valanse.service.PointService.PointService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoteServiceImplTest {

    @InjectMocks
    private VoteServiceImpl voteService;

    @Mock private VoteRepository voteRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private MemberVoteOptionRepository memberVoteOptionRepository;
    @Mock private CommentGroupRepository commentGroupRepository;
    @Mock private VoteOptionRepository voteOptionRepository;
    @Mock private MemberProfileRepository memberProfileRepository;
    @Mock private MemberProfileTitleRepository memberProfileTitleRepository;
    @Mock private PointService pointService;

    // ──────────────────────────────────────────────
    // createVote
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("투표 제목이 비어있으면 예외가 발생한다")
    void 제목길이실패_빈제목() {
        when(memberRepository.findByIdAndDeletedAtIsNull(any())).thenReturn(Optional.of(new Member()));

        VoteCreateRequest request = VoteCreateRequest.builder().title("").build();

        ApiException ex = assertThrows(ApiException.class, () -> voteService.createVote(1L, request));
        assertThat(ex.getMessage()).isEqualTo("투표 제목은 1자 이상 25자 이하여야 합니다 (공백 제외).");
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("공백만으로 된 제목은 비어있는 것으로 처리된다")
    void 제목길이실패_공백만있는제목() {
        when(memberRepository.findByIdAndDeletedAtIsNull(any())).thenReturn(Optional.of(new Member()));

        VoteCreateRequest request = VoteCreateRequest.builder().title("   ").build();

        ApiException ex = assertThrows(ApiException.class, () -> voteService.createVote(1L, request));
        assertThat(ex.getMessage()).isEqualTo("투표 제목은 1자 이상 25자 이하여야 합니다 (공백 제외).");
    }

    @Test
    @DisplayName("투표 제목이 25자를 초과하면 예외가 발생한다")
    void 제목길이실패_26자() {
        when(memberRepository.findByIdAndDeletedAtIsNull(any())).thenReturn(Optional.of(new Member()));

        VoteCreateRequest request = VoteCreateRequest.builder()
                .title("123456789012345678901234567890") // 30자
                .build();

        ApiException ex = assertThrows(ApiException.class, () -> voteService.createVote(1L, request));
        assertThat(ex.getMessage()).isEqualTo("투표 제목은 1자 이상 25자 이하여야 합니다 (공백 제외).");
    }

    @Test
    @DisplayName("투표 옵션이 null이면 예외가 발생한다")
    void 투표옵션실패_null() {
        when(memberRepository.findByIdAndDeletedAtIsNull(any())).thenReturn(Optional.of(new Member()));

        VoteCreateRequest request = VoteCreateRequest.builder()
                .title("테스트 투표").options(null).build();

        ApiException ex = assertThrows(ApiException.class, () -> voteService.createVote(1L, request));
        assertThat(ex.getMessage()).isEqualTo("투표 옵션은 1개 이상 4개 이하여야 합니다.");
    }

    @Test
    @DisplayName("투표 옵션이 5개 이상이면 예외가 발생한다")
    void 투표옵션실패_5개() {
        when(memberRepository.findByIdAndDeletedAtIsNull(any())).thenReturn(Optional.of(new Member()));

        VoteCreateRequest request = VoteCreateRequest.builder()
                .title("테스트 투표")
                .options(List.of("1", "2", "3", "4", "5"))
                .build();

        ApiException ex = assertThrows(ApiException.class, () -> voteService.createVote(1L, request));
        assertThat(ex.getMessage()).isEqualTo("투표 옵션은 1개 이상 4개 이하여야 합니다.");
    }

    @Test
    @DisplayName("투표 생성 시 voteRepository, commentGroupRepository, pointService가 각 1회 호출된다")
    void 투표생성_성공() {
        Member member = new Member();

        VoteCreateRequest request = VoteCreateRequest.builder()
                .title("테스트용 투표")
                .options(List.of("1번", "2번", "3번", "4번"))
                .category(VoteCategory.ALL)
                .build();

        Vote savedVote = Vote.builder()
                .id(100L).title(request.getTitle())
                .category(request.getCategory()).member(member).build();

        when(memberRepository.findByIdAndDeletedAtIsNull(any())).thenReturn(Optional.of(member));
        when(voteRepository.save(any())).thenReturn(savedVote);

        Long voteId = voteService.createVote(1L, request);

        assertThat(voteId).isEqualTo(100L);

        ArgumentCaptor<Vote> voteCaptor = ArgumentCaptor.forClass(Vote.class);
        verify(voteRepository).save(voteCaptor.capture());
        assertThat(voteCaptor.getValue().getVoteOptions()).hasSize(4);

        verify(commentGroupRepository).save(any(CommentGroup.class));
        verify(pointService, times(1)).givePoint(any(), any());
    }

    // ──────────────────────────────────────────────
    // processVote
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("처음 투표 시 카운트가 증가하고 isVoted=true를 반환한다")
    void 처음투표하기() {
        Member member = Member.builder().id(1L).build();
        Member postOwner = Member.builder().id(99L).build();
        Vote vote = Vote.builder().id(10L).totalVoteCount(7).member(postOwner).build();
        VoteOption voteOption = VoteOption.builder().id(100L).voteCount(3).vote(vote).build();

        when(memberRepository.findByIdAndDeletedAtIsNull(any())).thenReturn(Optional.of(member));
        when(voteRepository.findById(any())).thenReturn(Optional.of(vote));
        when(voteOptionRepository.findById(any())).thenReturn(Optional.of(voteOption));
        when(memberVoteOptionRepository.findByMemberIdAndVoteId(any(), any())).thenReturn(Optional.empty());

        VoteCancleResponseDto result = voteService.processVote(1L, 10L, 100L);

        assertThat(result.getTotalVoteCount()).isEqualTo(8);
        assertThat(result.getVoteOptionCount()).isEqualTo(4);
        assertThat(result.isVoted()).isTrue();
    }

    @Test
    @DisplayName("처음 투표 시 본인 게시물이 아니면 작성자에게 포인트가 지급된다")
    void 처음투표_포인트지급() {
        Member voter = Member.builder().id(1L).build();
        Member postOwner = Member.builder().id(99L).build();
        Vote vote = Vote.builder().id(10L).totalVoteCount(0).member(postOwner).build();
        VoteOption option = VoteOption.builder().id(100L).voteCount(0).vote(vote).build();

        when(memberRepository.findByIdAndDeletedAtIsNull(any())).thenReturn(Optional.of(voter));
        when(voteRepository.findById(any())).thenReturn(Optional.of(vote));
        when(voteOptionRepository.findById(any())).thenReturn(Optional.of(option));
        when(memberVoteOptionRepository.findByMemberIdAndVoteId(any(), any())).thenReturn(Optional.empty());

        voteService.processVote(1L, 10L, 100L);

        verify(pointService, times(1)).givePoint(eq(99L), any());
    }

    @Test
    @DisplayName("본인 게시물에 투표해도 포인트가 지급되지 않는다")
    void 처음투표_본인게시물_포인트미지급() {
        Member member = Member.builder().id(1L).build();
        Vote vote = Vote.builder().id(10L).totalVoteCount(0).member(member).build(); // 본인 게시물
        VoteOption option = VoteOption.builder().id(100L).voteCount(0).vote(vote).build();

        when(memberRepository.findByIdAndDeletedAtIsNull(any())).thenReturn(Optional.of(member));
        when(voteRepository.findById(any())).thenReturn(Optional.of(vote));
        when(voteOptionRepository.findById(any())).thenReturn(Optional.of(option));
        when(memberVoteOptionRepository.findByMemberIdAndVoteId(any(), any())).thenReturn(Optional.empty());

        voteService.processVote(1L, 10L, 100L);

        verify(pointService, never()).givePoint(any(), any());
    }

    @Test
    @DisplayName("동일 선택지를 다시 클릭하면 투표가 취소되고 카운트가 감소한다")
    void 투표취소() {
        Member member = Member.builder().id(1L).build();
        Vote vote = Vote.builder().id(10L).totalVoteCount(7).build();
        VoteOption voteOption = VoteOption.builder().id(100L).voteCount(3).vote(vote).build();
        MemberVoteOption mvo = MemberVoteOption.builder().voteOption(voteOption).build();

        when(memberRepository.findByIdAndDeletedAtIsNull(any())).thenReturn(Optional.of(member));
        when(voteRepository.findById(any())).thenReturn(Optional.of(vote));
        when(voteOptionRepository.findById(any())).thenReturn(Optional.of(voteOption));
        when(memberVoteOptionRepository.findByMemberIdAndVoteId(any(), any())).thenReturn(Optional.of(mvo));

        VoteCancleResponseDto result = voteService.processVote(1L, 10L, 100L);

        assertThat(result.getTotalVoteCount()).isEqualTo(6);
        assertThat(result.getVoteOptionCount()).isEqualTo(2);
        assertThat(result.isVoted()).isFalse();
        verify(memberVoteOptionRepository, times(1)).delete(any(MemberVoteOption.class));
    }

    @Test
    @DisplayName("다른 선택지로 재선택하면 기존 카운트가 감소하고 새 카운트가 증가한다")
    void 투표재선택() {
        Member member = Member.builder().id(1L).build();
        Vote vote = Vote.builder().id(10L).totalVoteCount(7).build();
        VoteOption oldOption = VoteOption.builder().id(100L).voteCount(3).vote(vote).build();
        VoteOption newOption = VoteOption.builder().id(101L).voteCount(10).vote(vote).build();
        MemberVoteOption mvo = MemberVoteOption.builder().voteOption(oldOption).build();

        when(memberRepository.findByIdAndDeletedAtIsNull(any())).thenReturn(Optional.of(member));
        when(voteRepository.findById(any())).thenReturn(Optional.of(vote));
        when(voteOptionRepository.findById(any())).thenReturn(Optional.of(newOption));
        when(memberVoteOptionRepository.findByMemberIdAndVoteId(any(), any())).thenReturn(Optional.of(mvo));

        VoteCancleResponseDto result = voteService.processVote(1L, 10L, 101L);

        assertThat(result.getTotalVoteCount()).isEqualTo(7); // 총 투표 수 변동 없음
        assertThat(result.getVoteOptionCount()).isEqualTo(11); // 새 옵션 +1
        assertThat(result.isVoted()).isTrue();
        assertThat(oldOption.getVoteCount()).isEqualTo(2); // 기존 옵션 -1
        verify(memberVoteOptionRepository, times(1)).delete(any(MemberVoteOption.class));
        verify(memberVoteOptionRepository, times(1)).save(any(MemberVoteOption.class));
    }

    // ──────────────────────────────────────────────
    // getVoteDetailById
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("존재하지 않는 투표 ID 조회 시 404 예외가 발생한다")
    void 투표상세조회_없는ID() {
        when(voteRepository.findById(any())).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> voteService.getVoteDetailById(999L));
        assertThat(ex.getMessage()).isEqualTo("투표를 찾을 수 없습니다.");
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("비로그인 사용자는 hasVoted=false로 투표 상세를 조회할 수 있다")
    void 투표상세조회_비로그인() {
        // SecurityContext를 익명 사용자로 설정
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("anonymousUser");
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);

        VoteOption option = VoteOption.builder()
                .id(1L).content("A선택지").voteCount(5).label(VoteLabel.A).build();
        Vote vote = Vote.builder()
                .id(10L).title("테스트 투표").totalVoteCount(10).build();
        vote.addVoteOption(option);

        when(voteRepository.findById(10L)).thenReturn(Optional.of(vote));

        VoteDetailResponse result = voteService.getVoteDetailById(10L);

        assertThat(result.getHasVoted()).isFalse();
        assertThat(result.getVotedOptionLabel()).isNull();
        assertThat(result.getOptions()).hasSize(1);
    }

    @Test
    @DisplayName("로그인 사용자가 투표한 경우 hasVoted=true와 선택지 라벨을 반환한다")
    void 투표상세조회_이미투표한경우() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("1");
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);

        VoteOption option = VoteOption.builder()
                .id(100L).content("A선택지").voteCount(5).label(VoteLabel.A).build();
        Vote vote = Vote.builder()
                .id(10L).title("테스트 투표").totalVoteCount(10).build();
        vote.addVoteOption(option);

        MemberVoteOption mvo = MemberVoteOption.builder().voteOption(option).build();

        when(voteRepository.findById(10L)).thenReturn(Optional.of(vote));
        when(memberVoteOptionRepository.findByMemberIdAndVoteId(1L, 10L)).thenReturn(Optional.of(mvo));

        VoteDetailResponse result = voteService.getVoteDetailById(10L);

        assertThat(result.getHasVoted()).isTrue();
        assertThat(result.getVotedOptionLabel()).isEqualTo("A");
    }

    // ──────────────────────────────────────────────
    // deleteVote
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("본인이 만든 투표를 삭제하면 softDelete가 적용된다")
    void 본인_투표삭제_성공() {
        Member member = Member.builder().id(1L).role(Role.USER).build();
        Vote vote = Vote.builder().id(10L).member(member).build();

        when(voteRepository.findById(any())).thenReturn(Optional.of(vote));
        when(memberRepository.findByIdAndDeletedAtIsNull(any())).thenReturn(Optional.of(member));

        voteService.deleteVote(1L, 10L);

        ArgumentCaptor<Vote> captor = ArgumentCaptor.forClass(Vote.class);
        verify(voteRepository).save(captor.capture());
        assertThat(captor.getValue().getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("관리자는 다른 사용자의 투표를 삭제할 수 있다")
    void 관리자_타인투표_삭제성공() {
        Member admin = Member.builder().id(1L).role(Role.ADMIN).build();
        Member writer = Member.builder().id(2L).role(Role.USER).build();
        Vote vote = Vote.builder().id(10L).member(writer).build();

        when(voteRepository.findById(any())).thenReturn(Optional.of(vote));
        when(memberRepository.findByIdAndDeletedAtIsNull(any())).thenReturn(Optional.of(admin));

        voteService.deleteVote(1L, 10L);

        ArgumentCaptor<Vote> captor = ArgumentCaptor.forClass(Vote.class);
        verify(voteRepository).save(captor.capture());
        assertThat(captor.getValue().getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("일반 사용자가 다른 사람의 투표를 삭제하려 하면 403 예외가 발생한다")
    void 일반사용자_타인투표_삭제실패() {
        Member user = Member.builder().id(1L).role(Role.USER).build();
        Member writer = Member.builder().id(2L).role(Role.USER).build();
        Vote vote = Vote.builder().id(10L).member(writer).build();

        when(voteRepository.findById(any())).thenReturn(Optional.of(vote));
        when(memberRepository.findByIdAndDeletedAtIsNull(any())).thenReturn(Optional.of(user));

        ApiException ex = assertThrows(ApiException.class, () -> voteService.deleteVote(1L, 10L));
        assertThat(ex.getMessage()).isEqualTo("삭제 권한이 없습니다.");
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ──────────────────────────────────────────────
    // updatePinStatus
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("일반 사용자가 핀 설정을 시도하면 403 예외가 발생한다")
    void 핀설정_권한없음() {
        Member user = Member.builder().id(1L).role(Role.USER).build();

        ApiException ex = assertThrows(ApiException.class,
                () -> voteService.updatePinStatus(user, 10L, PinType.HOT));
        assertThat(ex.getMessage()).isEqualTo("권한이 없습니다.");
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("관리자가 HOT 핀 설정 시 기존 HOT 게시물의 핀이 해제된다")
    void 핀설정_기존핀해제() {
        Member admin = Member.builder().id(1L).role(Role.ADMIN).build();

        Vote existingHot = Vote.builder().id(99L).build();
        existingHot.pin(PinType.HOT);
        Vote newVote = Vote.builder().id(10L).build();

        when(voteRepository.findById(10L)).thenReturn(Optional.of(newVote));
        when(voteRepository.findByPinType(PinType.HOT)).thenReturn(Optional.of(existingHot));

        voteService.updatePinStatus(admin, 10L, PinType.HOT);

        assertThat(existingHot.getPinType()).isEqualTo(PinType.NONE); // 기존 게시물 핀 해제
        assertThat(newVote.getPinType()).isEqualTo(PinType.HOT);      // 새 게시물 핀 설정
        verify(voteRepository, times(2)).save(any(Vote.class));
    }

    @Test
    @DisplayName("관리자가 NONE으로 설정하면 핀이 해제된다")
    void 핀해제() {
        Member admin = Member.builder().id(1L).role(Role.ADMIN).build();
        Vote vote = Vote.builder().id(10L).build();
        vote.pin(PinType.HOT);

        when(voteRepository.findById(10L)).thenReturn(Optional.of(vote));

        voteService.updatePinStatus(admin, 10L, PinType.NONE);

        assertThat(vote.getPinType()).isEqualTo(PinType.NONE);
        verify(voteRepository, times(1)).save(vote);
    }
}
