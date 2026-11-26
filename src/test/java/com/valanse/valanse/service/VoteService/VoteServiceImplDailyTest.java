package com.valanse.valanse.service.VoteService;

import com.valanse.valanse.domain.DailyVote;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.Vote;
import com.valanse.valanse.domain.VoteOption;
import com.valanse.valanse.domain.enums.PointType;
import com.valanse.valanse.domain.mapping.MemberVoteOption;
import com.valanse.valanse.dto.DailyVote.DailyVoteReward;
import com.valanse.valanse.dto.DailyVote.DailyVoteStatus;
import com.valanse.valanse.dto.DailyVote.DailyVoteTally;
import com.valanse.valanse.dto.Vote.VoteCancleResponseDto;
import com.valanse.valanse.repository.*;
import com.valanse.valanse.service.DailyVoteService.DailyVoteLifecycleService;
import com.valanse.valanse.service.DailyVoteService.DailyVoteParticipationService;
import com.valanse.valanse.service.PointService.PointService;
import com.valanse.valanse.service.StorageService.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoteServiceImplDailyTest {

    @InjectMocks
    private VoteServiceImpl voteService;

    @Mock private VoteRepository voteRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private MemberVoteOptionRepository memberVoteOptionRepository;
    @Mock private CommentGroupRepository commentGroupRepository;
    @Mock private VoteOptionRepository voteOptionRepository;
    @Mock private MemberProfileRepository memberProfileRepository;
    @Mock private MemberProfileTitleRepository memberProfileTitleRepository;
    @Mock private MemberBlockRepository memberBlockRepository;
    @Mock private PointService pointService;
    @Mock private StorageService storageService;
    @Mock private Clock clock;
    @Mock private DailyVoteParticipationService dailyVoteParticipationService;
    @Mock private DailyVoteLifecycleService dailyVoteLifecycleService;

    private static final Long MEMBER_ID = 1L;
    private static final Long VOTE_ID = 10L;
    private static final Long OPTION_A_ID = 100L;
    private static final Long OPTION_B_ID = 101L;

    private Member member;
    private Vote vote;
    private VoteOption optionA;
    private VoteOption optionB;
    private DailyVote today;

    @BeforeEach
    void setUp() {
        member = Member.builder().id(MEMBER_ID).build();
        Member postOwner = Member.builder().id(99L).build();
        vote = Vote.builder().id(VOTE_ID).totalVoteCount(7).member(postOwner).build();
        optionA = VoteOption.builder().id(OPTION_A_ID).voteCount(3).vote(vote).build();
        optionB = VoteOption.builder().id(OPTION_B_ID).voteCount(4).vote(vote).build();
        today = DailyVote.builder()
                .voteDate(LocalDate.of(2026, 11, 26))
                .voteId(VOTE_ID)
                .optionAId(OPTION_A_ID)
                .optionBId(OPTION_B_ID)
                .build();

        when(memberRepository.findByIdAndDeletedAtIsNull(MEMBER_ID)).thenReturn(Optional.of(member));
        when(voteRepository.findById(VOTE_ID)).thenReturn(Optional.of(vote));
        lenient().when(voteOptionRepository.findById(OPTION_A_ID)).thenReturn(Optional.of(optionA));
        lenient().when(voteOptionRepository.findById(OPTION_B_ID)).thenReturn(Optional.of(optionB));
    }

    private void stubToday() {
        when(dailyVoteParticipationService.findTodayFor(VOTE_ID)).thenReturn(Optional.of(today));
    }

    @Test
    @DisplayName("오늘의 게임이 아니면 기존 투표 동작 그대로이고 daily 는 null 이다")
    void 오늘게임이_아니면_기존동작_그대로이고_daily는_null() {
        when(dailyVoteParticipationService.findTodayFor(VOTE_ID)).thenReturn(Optional.empty());
        when(memberVoteOptionRepository.findByMemberIdAndVoteId(MEMBER_ID, VOTE_ID)).thenReturn(Optional.empty());

        VoteCancleResponseDto result = voteService.processVote(MEMBER_ID, VOTE_ID, OPTION_A_ID);

        assertThat(result.isVoted()).isTrue();
        assertThat(result.getTotalVoteCount()).isEqualTo(8);
        assertThat(result.getVoteOptionId()).isEqualTo(OPTION_A_ID);
        assertThat(result.getVoteOptionCount()).isEqualTo(4);
        assertThat(result.getDaily()).isNull();
        verify(memberVoteOptionRepository).save(any(MemberVoteOption.class));
        verify(pointService).givePoint(99L, PointType.POST_VOTED);
        verify(dailyVoteParticipationService).findTodayFor(VOTE_ID);
        verifyNoMoreInteractions(dailyVoteParticipationService);
    }

    @Test
    @DisplayName("예전에 투표한 사람이 오늘 같은 쪽을 처음 누르면 취소되지 않고 오늘 참여로 기록된다")
    void 예전_투표자가_같은쪽을_처음_누르면_취소되지_않고_참여된다() {
        stubToday();
        MemberVoteOption mvo = MemberVoteOption.builder().member(member).vote(vote).voteOption(optionA).build();
        when(memberVoteOptionRepository.findByMemberIdAndVoteId(MEMBER_ID, VOTE_ID)).thenReturn(Optional.of(mvo));
        when(dailyVoteParticipationService.hasParticipated(today, MEMBER_ID)).thenReturn(false);
        when(dailyVoteParticipationService.participate(today, member, OPTION_A_ID))
                .thenReturn(new DailyVoteReward(3, false));
        when(dailyVoteParticipationService.tally(today)).thenReturn(new DailyVoteTally(1, 0));

        VoteCancleResponseDto result = voteService.processVote(MEMBER_ID, VOTE_ID, OPTION_A_ID);

        assertThat(result.isVoted()).isTrue();
        assertThat(result.getTotalVoteCount()).isEqualTo(7);
        assertThat(result.getVoteOptionCount()).isEqualTo(3);
        assertThat(optionA.getVoteCount()).isEqualTo(3);
        assertThat(mvo.getVoteOption()).isSameAs(optionA);
        verify(memberVoteOptionRepository, never()).delete(any(MemberVoteOption.class));
        verify(dailyVoteParticipationService, times(1)).participate(today, member, OPTION_A_ID);
        verify(dailyVoteParticipationService, never()).updateCurrentOption(any(), anyLong(), any());
        verify(pointService, never()).givePoint(any(), any());
    }

    @Test
    @DisplayName("예전에 투표한 사람이 오늘 다른 쪽을 처음 누르면 재선택된 뒤 새 선택으로 참여된다")
    void 예전_투표자가_다른쪽을_처음_누르면_재선택후_참여된다() {
        stubToday();
        MemberVoteOption mvo = MemberVoteOption.builder().member(member).vote(vote).voteOption(optionA).build();
        when(memberVoteOptionRepository.findByMemberIdAndVoteId(MEMBER_ID, VOTE_ID)).thenReturn(Optional.of(mvo));
        when(dailyVoteParticipationService.hasParticipated(today, MEMBER_ID)).thenReturn(false);
        when(dailyVoteParticipationService.participate(today, member, OPTION_B_ID))
                .thenReturn(new DailyVoteReward(3, false));
        when(dailyVoteParticipationService.tally(today)).thenReturn(new DailyVoteTally(0, 1));

        VoteCancleResponseDto result = voteService.processVote(MEMBER_ID, VOTE_ID, OPTION_B_ID);

        assertThat(result.isVoted()).isTrue();
        assertThat(result.getTotalVoteCount()).isEqualTo(7);
        assertThat(result.getVoteOptionCount()).isEqualTo(5);
        assertThat(optionA.getVoteCount()).isEqualTo(2);
        assertThat(mvo.getVoteOption()).isSameAs(optionB);
        verify(memberVoteOptionRepository, never()).delete(any(MemberVoteOption.class));
        verify(dailyVoteParticipationService, times(1)).participate(today, member, OPTION_B_ID);
        verify(dailyVoteParticipationService, never()).updateCurrentOption(any(), anyLong(), any());
    }

    @Test
    @DisplayName("오늘 참여한 뒤 취소하고 다시 투표해도 참여 포인트는 한 번만 지급되고 현재 선택만 바뀐다")
    void 참여후_취소하고_다시_투표해도_포인트는_한번만() {
        stubToday();
        MemberVoteOption mvo = MemberVoteOption.builder().member(member).vote(vote).voteOption(optionA).build();
        when(memberVoteOptionRepository.findByMemberIdAndVoteId(MEMBER_ID, VOTE_ID))
                .thenReturn(Optional.of(mvo), Optional.empty());
        when(dailyVoteParticipationService.hasParticipated(today, MEMBER_ID)).thenReturn(true);
        when(dailyVoteParticipationService.tally(today))
                .thenReturn(new DailyVoteTally(0, 0), new DailyVoteTally(1, 0));

        VoteCancleResponseDto cancel = voteService.processVote(MEMBER_ID, VOTE_ID, OPTION_A_ID);
        VoteCancleResponseDto revote = voteService.processVote(MEMBER_ID, VOTE_ID, OPTION_A_ID);

        assertThat(cancel.isVoted()).isFalse();
        assertThat(cancel.getTotalVoteCount()).isEqualTo(6);
        assertThat(cancel.getDaily()).isEqualTo(new DailyVoteStatus(new DailyVoteReward(0, true), new DailyVoteTally(0, 0)));
        assertThat(revote.isVoted()).isTrue();
        assertThat(revote.getTotalVoteCount()).isEqualTo(7);
        assertThat(revote.getDaily()).isEqualTo(new DailyVoteStatus(new DailyVoteReward(0, true), new DailyVoteTally(1, 0)));
        verify(memberVoteOptionRepository, times(1)).delete(mvo);
        verify(dailyVoteParticipationService, never()).participate(any(), any(), any());

        InOrder inOrder = inOrder(dailyVoteParticipationService);
        inOrder.verify(dailyVoteParticipationService).updateCurrentOption(today, MEMBER_ID, null);
        inOrder.verify(dailyVoteParticipationService).updateCurrentOption(today, MEMBER_ID, OPTION_A_ID);
    }

    @Test
    @DisplayName("오늘의 게임인지는 KST 날짜를 쓰는 findTodayFor 에 맡기고 투표 서비스는 시계를 직접 보지 않는다")
    void 날짜는_KST_기준으로_판단한다() {
        stubToday();
        when(memberVoteOptionRepository.findByMemberIdAndVoteId(MEMBER_ID, VOTE_ID)).thenReturn(Optional.empty());
        when(dailyVoteParticipationService.hasParticipated(today, MEMBER_ID)).thenReturn(false);
        when(dailyVoteParticipationService.participate(today, member, OPTION_A_ID))
                .thenReturn(new DailyVoteReward(3, false));
        when(dailyVoteParticipationService.tally(today)).thenReturn(new DailyVoteTally(1, 0));

        voteService.processVote(MEMBER_ID, VOTE_ID, OPTION_A_ID);

        verify(dailyVoteParticipationService).findTodayFor(VOTE_ID);
        verify(dailyVoteParticipationService).participate(same(today), same(member), eq(OPTION_A_ID));
        verifyNoInteractions(clock);
    }

    @Test
    @DisplayName("오늘의 게임에 처음 투표하면 응답에 지급 보상과 이번 요청이 반영된 오늘 표가 담긴다")
    void 응답에_오늘표와_보상이_담긴다() {
        stubToday();
        when(memberVoteOptionRepository.findByMemberIdAndVoteId(MEMBER_ID, VOTE_ID)).thenReturn(Optional.empty());
        when(dailyVoteParticipationService.hasParticipated(today, MEMBER_ID)).thenReturn(false);
        when(dailyVoteParticipationService.participate(today, member, OPTION_B_ID))
                .thenReturn(new DailyVoteReward(3, false));
        when(dailyVoteParticipationService.tally(today)).thenReturn(new DailyVoteTally(5, 2));

        VoteCancleResponseDto result = voteService.processVote(MEMBER_ID, VOTE_ID, OPTION_B_ID);

        assertThat(result.isVoted()).isTrue();
        assertThat(result.getTotalVoteCount()).isEqualTo(8);
        assertThat(result.getVoteOptionCount()).isEqualTo(5);
        assertThat(result.getDaily().reward()).isEqualTo(new DailyVoteReward(3, false));
        assertThat(result.getDaily().todayTally()).isEqualTo(new DailyVoteTally(5, 2));
        verify(pointService).givePoint(99L, PointType.POST_VOTED);

        InOrder inOrder = inOrder(dailyVoteParticipationService);
        inOrder.verify(dailyVoteParticipationService).participate(today, member, OPTION_B_ID);
        inOrder.verify(dailyVoteParticipationService).tally(today);
    }
}
