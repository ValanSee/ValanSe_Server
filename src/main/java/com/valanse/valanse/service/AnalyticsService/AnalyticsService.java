package com.valanse.valanse.service.AnalyticsService;

import com.valanse.valanse.dto.Analytics.MauResponse;
import com.valanse.valanse.dto.Analytics.PageViewEventRequest;
import com.valanse.valanse.dto.Analytics.PageViewEventResponse;
import org.springframework.security.core.Authentication;

/**
 * AnalyticsService 기능의 비즈니스 계약을 정의하는 서비스 인터페이스 코드입니다.
 */
public interface AnalyticsService {

    PageViewEventResponse recordPageView(PageViewEventRequest request, Authentication authentication);

    MauResponse getMonthlyActiveUsers(String yearMonth);
}
