package com.valanse.valanse.service.ContentSeedService;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

public record GeneratedPostBatch(
        @JsonPropertyDescription("요청받은 개수만큼 생성한 게시글 목록")
        List<GeneratedPost> posts
) {
}
