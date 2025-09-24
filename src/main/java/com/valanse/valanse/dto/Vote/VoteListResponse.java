
package com.valanse.valanse.dto.Vote;

import com.valanse.valanse.domain.enums.VoteCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;



@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoteListResponse {
    private List<VoteDto> votes;
    private boolean has_next_page;
    private String next_cursor; // 다음 페이지 요청에 사용할 커서 값// 다음 페이지 존재 여부

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VoteDto {
        private Long id;
        private String title;
        private String category; // ENUM 이름을 String으로 반환
        private Long member_id;
        private String nickname;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private LocalDateTime created_at;
        private Integer total_vote_count;
        private Integer total_comment_count; // 댓글 그룹의 총 댓글 수
        private List<VoteOptionListDto> options; // 옵션 목록

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VoteOptionListDto {
        private Long id;
        private String content;
    }
}