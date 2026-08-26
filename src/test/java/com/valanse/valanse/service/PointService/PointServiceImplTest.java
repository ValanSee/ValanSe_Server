package com.valanse.valanse.service.PointService;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.common.message.MemberErrorMessage;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.MemberProfile;
import com.valanse.valanse.domain.PointHistory;
import com.valanse.valanse.domain.enums.PointType;
import com.valanse.valanse.dto.PointHistory.PointHistoryResponse;
import com.valanse.valanse.repository.MemberProfileRepository;
import com.valanse.valanse.repository.MemberRepository;
import com.valanse.valanse.repository.PointHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointServiceImplTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberProfileRepository memberProfileRepository;

    @Mock
    private PointHistoryRepository pointHistoryRepository;

    @InjectMocks
    private PointServiceImpl pointService;

    @Test
    @DisplayName("포인트 히스토리 조회 - 성공")
    void getPointHistory_Success() {
        // Given
        Long memberId = 1L;
        Member member = Member.builder().id(memberId).build();

        List<PointHistory> histories = Arrays.asList(
            PointHistory.builder()
                .id(1L)
                .member(member)
                .amount(40L)
                .remainingPoint(40L)
                .type(PointType.SIGN_UP)
                .build(),
            PointHistory.builder()
                .id(2L)
                .member(member)
                .amount(5L)
                .remainingPoint(45L)
                .type(PointType.POST_CREATE)
                .build(),
            PointHistory.builder()
                .id(3L)
                .member(member)
                .amount(1L)
                .remainingPoint(46L)
                .type(PointType.COMMENT_CREATE)
                .build()
        );

        when(memberRepository.findByIdAndDeletedAtIsNull(memberId))
            .thenReturn(Optional.of(member));
        when(pointHistoryRepository.findByMemberId(memberId))
            .thenReturn(histories);

        // When
        PointHistoryResponse response = pointService.getPointHistory(memberId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.pointHistory()).hasSize(3);

        PointHistoryResponse.PointHistoryItem firstItem = response.pointHistory().get(0);
        assertThat(firstItem.amount()).isEqualTo(40L);
        assertThat(firstItem.remainingPoint()).isEqualTo(40L);
        assertThat(firstItem.type()).isEqualTo(PointType.SIGN_UP);
        assertThat(firstItem.typeDescription()).isEqualTo("회원가입");
    }

    @Test
    @DisplayName("포인트 히스토리 조회 - 회원이 존재하지 않는 경우")
    void getPointHistory_MemberNotFound() {
        // Given
        Long memberId = 999L;
        when(memberRepository.findByIdAndDeletedAtIsNull(memberId))
            .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> pointService.getPointHistory(memberId))
            .isInstanceOf(ApiException.class)
            .hasMessage(MemberErrorMessage.MEMBER_NOT_FOUND.message());
    }

    @Test
    @DisplayName("포인트 타입 설명 확인")
    void getPointTypeDescription_AllTypes() {
        // Given
        Long memberId = 1L;
        Member member = Member.builder().id(memberId).build();

        List<PointHistory> histories = Arrays.asList(
            PointHistory.builder().id(1L).member(member).amount(40L).type(PointType.SIGN_UP).build(),
            PointHistory.builder().id(2L).member(member).amount(5L).type(PointType.POST_CREATE).build(),
            PointHistory.builder().id(3L).member(member).amount(1L).type(PointType.COMMENT_CREATE).build(),
            PointHistory.builder().id(4L).member(member).amount(1L).type(PointType.POST_VOTED).build(),
            PointHistory.builder().id(5L).member(member).amount(50L).type(PointType.HOT_ISSUE).build(),
            PointHistory.builder().id(6L).member(member).amount(-300L).type(PointType.TITLE_PURCHASE).build()
        );

        when(memberRepository.findByIdAndDeletedAtIsNull(memberId))
            .thenReturn(Optional.of(member));
        when(pointHistoryRepository.findByMemberId(memberId))
            .thenReturn(histories);

        // When
        PointHistoryResponse response = pointService.getPointHistory(memberId);

        // Then
        List<PointHistoryResponse.PointHistoryItem> items = response.pointHistory();
        assertThat(items.get(0).typeDescription()).isEqualTo("회원가입");
        assertThat(items.get(1).typeDescription()).isEqualTo("게시글 작성");
        assertThat(items.get(2).typeDescription()).isEqualTo("댓글 작성");
        assertThat(items.get(3).typeDescription()).isEqualTo("투표 참여");
        assertThat(items.get(4).typeDescription()).isEqualTo("핫이슈");
        assertThat(items.get(5).typeDescription()).isEqualTo("칭호 구매");
    }

    @Test
    @DisplayName("포인트 사용 내역 기록 - 칭호 구매")
    void recordPointUsage_TitlePurchase() {
        // Given
        Long memberId = 1L;
        Member member = Member.builder().id(memberId).build();
        MemberProfile profile = MemberProfile.builder()
            .member(member)
            .point(700L)
            .build();

        when(memberRepository.findByIdAndDeletedAtIsNull(memberId))
            .thenReturn(Optional.of(member));
        when(memberProfileRepository.findByMemberId(memberId))
            .thenReturn(Optional.of(profile));

        // When
        pointService.recordPointUsage(memberId, 300L, PointType.TITLE_PURCHASE);

        // Then
        verify(pointHistoryRepository).save(argThat(history ->
            history.getMember() == member &&
                history.getAmount().equals(-300L) &&
                history.getRemainingPoint().equals(700L) &&
                history.getType() == PointType.TITLE_PURCHASE
        ));
    }

    @Test
    @DisplayName("createdAt 날짜 포맷 확인 - YYYY-MM-DD HH:mm:ss 형식")
    void getPointHistory_DateFormatting() {
        // Given
        Long memberId = 1L;
        Member member = Member.builder().id(memberId).build();

        LocalDateTime testDateTime = LocalDateTime.of(2026, 4, 26, 15, 30, 22);

        // PointHistory 객체를 mock으로 생성
        PointHistory mockHistory = mock(PointHistory.class);
        when(mockHistory.getId()).thenReturn(1L);
        when(mockHistory.getAmount()).thenReturn(40L);
        when(mockHistory.getRemainingPoint()).thenReturn(40L);
        when(mockHistory.getType()).thenReturn(PointType.SIGN_UP);
        when(mockHistory.getCreatedAt()).thenReturn(testDateTime);

        List<PointHistory> histories = Arrays.asList(mockHistory);

        when(memberRepository.findByIdAndDeletedAtIsNull(memberId))
            .thenReturn(Optional.of(member));
        when(pointHistoryRepository.findByMemberId(memberId))
            .thenReturn(histories);

        // When
        PointHistoryResponse response = pointService.getPointHistory(memberId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.pointHistory()).hasSize(1);

        PointHistoryResponse.PointHistoryItem item = response.pointHistory().get(0);
        // 날짜가 "2026-04-26 15:30:22" 형식으로 포맷되었는지 확인
        assertThat(item.createdAt()).isEqualTo("2026-04-26 15:30:22");
        assertThat(item.amount()).isEqualTo(40L);
        assertThat(item.remainingPoint()).isEqualTo(40L);
        assertThat(item.type()).isEqualTo(PointType.SIGN_UP);
        assertThat(item.typeDescription()).isEqualTo("회원가입");
    }
}
