package com.valanse.valanse.service.AnalyticsService;

import com.valanse.valanse.domain.ActivityEvent;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.enums.ActivityEventType;
import com.valanse.valanse.dto.Analytics.PageViewEventRequest;
import com.valanse.valanse.repository.ActivityEventRepository;
import com.valanse.valanse.repository.AnonymousUserLinkRepository;
import com.valanse.valanse.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * anonymous_user_link 유니크 제약 위반 경쟁 상태(race condition) 회귀 테스트.
 * existsByAnonymousId 검사와 save 사이에 다른 요청이 먼저 연결을 저장하는 상황을
 * save()가 DataIntegrityViolationException을 던지는 것으로 재현한다.
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsServiceImplConcurrencyTest {

    @InjectMocks
    private AnalyticsServiceImpl analyticsService;

    @Mock private ActivityEventRepository activityEventRepository;
    @Mock private AnonymousUserLinkRepository anonymousUserLinkRepository;
    @Mock private MemberRepository memberRepository;

    @Mock private Authentication authentication;

    private Member member;

    @BeforeEach
    void setUp() {
        member = Member.builder().id(1L).build();
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("1");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
    }

    @Test
    @DisplayName("연결 저장 중 동시 요청으로 중복 키 예외가 발생해도 페이지뷰 기록은 실패하지 않는다")
    void recordPageView_DuplicateLinkExceptionDuringConcurrentRequest_DoesNotFail() {
        when(anonymousUserLinkRepository.existsByAnonymousId("anon-race")).thenReturn(false);
        when(anonymousUserLinkRepository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate"));
        when(activityEventRepository.save(any())).thenReturn(
                ActivityEvent.builder()
                        .id(1L)
                        .member(member)
                        .anonymousId("anon-race")
                        .eventType(ActivityEventType.PAGE_VIEW)
                        .pagePath("/votes")
                        .build());

        PageViewEventRequest request = new PageViewEventRequest("anon-race", "/votes");

        assertThatCode(() -> analyticsService.recordPageView(request, authentication))
                .doesNotThrowAnyException();

        verify(activityEventRepository).save(any());
        verify(activityEventRepository).linkAnonymousEventsToMember(eq("anon-race"), eq(member));
    }
}
