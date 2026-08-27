package com.valanse.valanse.service.ContentSeedService;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

public record GeneratedInteractionBatch(
        @JsonPropertyDescription("요청받은 개수만큼 생성한 투표+댓글 목록")
        List<GeneratedInteraction> interactions
) {
}
