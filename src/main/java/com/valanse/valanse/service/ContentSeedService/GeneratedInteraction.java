package com.valanse.valanse.service.ContentSeedService;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record GeneratedInteraction(
        @JsonPropertyDescription("투표하고 댓글을 남길 대상 게시글의 id (제공된 후보 목록 중 하나)")
        Long targetPostId,

        @JsonPropertyDescription("선택한 옵션. optionA를 골랐으면 A, optionB를 골랐으면 B")
        SelectedOption selectedOption,

        @JsonPropertyDescription("선택 이유가 담긴 최상위 댓글. 1~2문장, 최대 80자")
        String comment
) {
    public enum SelectedOption {
        A, B
    }
}
