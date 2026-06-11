package com.valanse.valanse.service.AnalyticsService;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.domain.ActivityEvent;
import com.valanse.valanse.domain.AnonymousUserLink;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.enums.ActivityEventType;
import com.valanse.valanse.dto.Analytics.MauResponse;
import com.valanse.valanse.dto.Analytics.PageViewEventRequest;
import com.valanse.valanse.dto.Analytics.PageViewEventResponse;
import com.valanse.valanse.repository.ActivityEventRepository;
import com.valanse.valanse.repository.AnonymousUserLinkRepository;
import com.valanse.valanse.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

@Service
@Transactional
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    private final ActivityEventRepository activityEventRepository;
    private final AnonymousUserLinkRepository anonymousUserLinkRepository;
    private final MemberRepository memberRepository;

    @Override
    public PageViewEventResponse recordPageView(PageViewEventRequest request, Authentication authentication) {
        if (request == null || !StringUtils.hasText(request.pagePath())) {
            throw new ApiException("pagePath는 필수입니다.", HttpStatus.BAD_REQUEST);
        }

        String anonymousId = normalize(request.anonymousId());
        Member member = resolveMember(authentication);

        if (member == null && !StringUtils.hasText(anonymousId)) {
            throw new ApiException("비로그인 페이지 방문 기록에는 anonymousId가 필요합니다.", HttpStatus.BAD_REQUEST);
        }

        if (member != null && StringUtils.hasText(anonymousId)) {
            linkAnonymousIdToMember(anonymousId, member);
        } else if (member == null && StringUtils.hasText(anonymousId)) {
            member = anonymousUserLinkRepository.findByAnonymousId(anonymousId)
                    .map(AnonymousUserLink::getMember)
                    .orElse(null);
        }

        ActivityEvent event = ActivityEvent.builder()
                .member(member)
                .anonymousId(anonymousId)
                .eventType(ActivityEventType.PAGE_VIEW)
                .pagePath(request.pagePath().trim())
                .build();

        ActivityEvent savedEvent = activityEventRepository.save(event);
        return new PageViewEventResponse(savedEvent.getId());
    }

    @Transactional(readOnly = true)
    @Override
    public MauResponse getMonthlyActiveUsers(String yearMonth) {
        YearMonth targetMonth = parseYearMonth(yearMonth);
        LocalDateTime startAt = targetMonth.atDay(1).atStartOfDay(KOREA_ZONE).toLocalDateTime();
        LocalDateTime endAt = targetMonth.plusMonths(1).atDay(1).atStartOfDay(KOREA_ZONE).toLocalDateTime();

        long anonymousMau = activityEventRepository.countAnonymousUsers(ActivityEventType.PAGE_VIEW, startAt, endAt);
        long loginMau = activityEventRepository.countActiveLoginUsers(ActivityEventType.PAGE_VIEW, startAt, endAt);
        long withdrawnMau = activityEventRepository.countWithdrawnUsers(ActivityEventType.PAGE_VIEW, startAt, endAt);
        long totalMau = anonymousMau + loginMau + withdrawnMau;

        return new MauResponse(targetMonth.toString(), anonymousMau, loginMau, withdrawnMau, totalMau);
    }

    private void linkAnonymousIdToMember(String anonymousId, Member member) {
        if (!anonymousUserLinkRepository.existsByAnonymousId(anonymousId)) {
            anonymousUserLinkRepository.save(AnonymousUserLink.builder()
                    .anonymousId(anonymousId)
                    .member(member)
                    .build());
        }
        activityEventRepository.linkAnonymousEventsToMember(anonymousId, member);
    }

    private Member resolveMember(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        try {
            Long memberId = Long.parseLong(authentication.getName());
            return memberRepository.findById(memberId).orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private YearMonth parseYearMonth(String yearMonth) {
        if (!StringUtils.hasText(yearMonth)) {
            return YearMonth.now(KOREA_ZONE);
        }
        try {
            return YearMonth.parse(yearMonth);
        } catch (DateTimeParseException e) {
            throw new ApiException("yearMonth는 yyyy-MM 형식이어야 합니다.", HttpStatus.BAD_REQUEST);
        }
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
