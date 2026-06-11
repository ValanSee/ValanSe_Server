package com.valanse.valanse.dto.Analytics;

public record PageViewEventRequest(
        String anonymousId,
        String pagePath
) {
}
