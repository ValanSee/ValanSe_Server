package com.valanse.valanse.service.ContentSeedService;

import java.util.List;

public record QualityCheckResult(boolean passed, List<String> reasons) {

    public static QualityCheckResult pass() {
        return new QualityCheckResult(true, List.of());
    }

    public static QualityCheckResult reject(List<String> reasons) {
        return new QualityCheckResult(false, List.copyOf(reasons));
    }
}
