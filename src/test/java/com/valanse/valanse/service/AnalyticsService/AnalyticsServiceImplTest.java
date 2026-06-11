package com.valanse.valanse.service.AnalyticsService;

import com.valanse.valanse.domain.ActivityEvent;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.enums.ActivityEventType;
import com.valanse.valanse.dto.Analytics.MauResponse;
import com.valanse.valanse.dto.Analytics.PageViewEventRequest;
import com.valanse.valanse.repository.ActivityEventRepository;
import com.valanse.valanse.repository.AnonymousUserLinkRepository;
import com.valanse.valanse.repository.MemberRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AnalyticsServiceImplTest {

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private ActivityEventRepository activityEventRepository;

    @Autowired
    private AnonymousUserLinkRepository anonymousUserLinkRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        activityEventRepository.deleteAll();
        anonymousUserLinkRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("비로그인 페이지 방문은 anonymousId 기준으로 기록된다")
    void recordPageView_AnonymousUser() {
        analyticsService.recordPageView(new PageViewEventRequest("anon-1", "/votes"), null);

        List<ActivityEvent> events = activityEventRepository.findAll();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getMember()).isNull();
        assertThat(events.get(0).getAnonymousId()).isEqualTo("anon-1");
        assertThat(events.get(0).getEventType()).isEqualTo(ActivityEventType.PAGE_VIEW);
        assertThat(events.get(0).getPagePath()).isEqualTo("/votes");
    }

    @Test
    @DisplayName("로그인 페이지 방문은 memberId와 anonymousId를 함께 기록한다")
    void recordPageView_LoginUser() {
        Member member = saveMember("kakao-login");

        analyticsService.recordPageView(
                new PageViewEventRequest("anon-login", "/member/profile"),
                authentication(member)
        );

        ActivityEvent event = activityEventRepository.findAll().get(0);
        assertThat(event.getMember().getId()).isEqualTo(member.getId());
        assertThat(event.getAnonymousId()).isEqualTo("anon-login");
    }

    @Test
    @DisplayName("같은 anonymousId가 로그인하면 이전 익명 방문도 로그인 사용자로 병합된다")
    void getMonthlyActiveUsers_MergesAnonymousEventsAfterLogin() {
        Member member = saveMember("kakao-merge");

        analyticsService.recordPageView(new PageViewEventRequest("anon-merge", "/"), null);
        analyticsService.recordPageView(new PageViewEventRequest("anon-merge", "/votes"), authentication(member));

        MauResponse response = analyticsService.getMonthlyActiveUsers(null);

        assertThat(response.anonymousMau()).isZero();
        assertThat(response.loginMau()).isEqualTo(1);
        assertThat(response.withdrawnMau()).isZero();
        assertThat(response.totalMau()).isEqualTo(1);
    }

    @Test
    @DisplayName("탈퇴 사용자는 조회 시 Member.deletedAt 기준으로 따로 집계된다")
    void getMonthlyActiveUsers_CountsWithdrawnUsersSeparately() {
        Member member = saveMember("kakao-withdrawn");
        analyticsService.recordPageView(new PageViewEventRequest("anon-withdrawn", "/votes/1"), authentication(member));

        member.softDelete();
        memberRepository.save(member);

        MauResponse response = analyticsService.getMonthlyActiveUsers(null);

        assertThat(response.anonymousMau()).isZero();
        assertThat(response.loginMau()).isZero();
        assertThat(response.withdrawnMau()).isEqualTo(1);
        assertThat(response.totalMau()).isEqualTo(1);
    }

    @Test
    @DisplayName("MAU는 한국 시간 기준 특정 월 시작 이상, 다음 달 시작 미만으로 집계된다")
    void getMonthlyActiveUsers_UsesKoreaMonthBoundary() {
        ActivityEvent included = activityEventRepository.save(ActivityEvent.builder()
                .anonymousId("anon-june")
                .eventType(ActivityEventType.PAGE_VIEW)
                .pagePath("/")
                .build());
        ActivityEvent excluded = activityEventRepository.save(ActivityEvent.builder()
                .anonymousId("anon-july")
                .eventType(ActivityEventType.PAGE_VIEW)
                .pagePath("/")
                .build());
        entityManager.flush();

        updateCreatedAt(included.getId(), LocalDateTime.of(2026, 6, 1, 0, 0));
        updateCreatedAt(excluded.getId(), LocalDateTime.of(2026, 7, 1, 0, 0));
        entityManager.flush();
        entityManager.clear();

        MauResponse response = analyticsService.getMonthlyActiveUsers("2026-06");

        assertThat(response.yearMonth()).isEqualTo("2026-06");
        assertThat(response.anonymousMau()).isEqualTo(1);
        assertThat(response.totalMau()).isEqualTo(1);
    }

    private Member saveMember(String socialId) {
        return memberRepository.save(Member.builder()
                .socialId(socialId)
                .email(socialId + "@example.com")
                .name("테스터")
                .profile_image_url("http://image.example.com/profile.png")
                .kakaoAccessToken("access")
                .kakaoRefreshToken("refresh")
                .build());
    }

    private UsernamePasswordAuthenticationToken authentication(Member member) {
        User user = new User(
                member.getId().toString(),
                "",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        return new UsernamePasswordAuthenticationToken(user, "", user.getAuthorities());
    }

    private void updateCreatedAt(Long eventId, LocalDateTime createdAt) {
        entityManager.createNativeQuery("UPDATE activity_event SET created_at = :createdAt WHERE id = :id")
                .setParameter("createdAt", createdAt)
                .setParameter("id", eventId)
                .executeUpdate();
    }
}
