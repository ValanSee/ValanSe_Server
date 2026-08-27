package com.valanse.valanse.service.ContentSeedService;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record GeneratedPost(
        @JsonPropertyDescription("trim 후 1~25자인 게시글 제목")
        String title,

        @JsonPropertyDescription("1~2문장, 최대 100자인 게시글 본문")
        String body,

        @JsonPropertyDescription("첫 번째 선택지. 공백이 아닌 텍스트, optionB와 중복 금지")
        String optionA,

        @JsonPropertyDescription("두 번째 선택지. 공백이 아닌 텍스트, optionA와 중복 금지")
        String optionB,

        @JsonPropertyDescription("밸런스게임 카테고리")
        GeneratableVoteCategory category
) {
}
