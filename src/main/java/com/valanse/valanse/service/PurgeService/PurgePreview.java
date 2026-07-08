package com.valanse.valanse.service.PurgeService;

import java.time.LocalDateTime;

public record PurgePreview(
        LocalDateTime cutoff,
        long expiredComments,
        long expiredVotes,
        long expiredMembers
) {
}
