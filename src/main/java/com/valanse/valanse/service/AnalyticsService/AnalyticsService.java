package com.valanse.valanse.service.AnalyticsService;

import com.valanse.valanse.dto.Analytics.MauResponse;
import com.valanse.valanse.dto.Analytics.PageViewEventRequest;
import com.valanse.valanse.dto.Analytics.PageViewEventResponse;
import org.springframework.security.core.Authentication;

public interface AnalyticsService {

    PageViewEventResponse recordPageView(PageViewEventRequest request, Authentication authentication);

    MauResponse getMonthlyActiveUsers(String yearMonth);
}
