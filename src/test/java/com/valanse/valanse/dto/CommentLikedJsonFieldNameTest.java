package com.valanse.valanse.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.valanse.valanse.dto.Comment.CommentLikeResponseDto;
import com.valanse.valanse.dto.Comment.CommentReplyResponseDto;
import com.valanse.valanse.dto.Comment.CommentResponseDto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 댓글/대댓글/좋아요 토글 응답의 좋아요 여부가 "liked"라는 JSON 필드명(boolean)으로 직렬화되는지 검증합니다.
 * boolean 필드 + Lombok @Getter 는 isLiked()를 만들기 때문에 실제 필드명을 직접 확인합니다.
 */
class CommentLikedJsonFieldNameTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void CommentResponseDto는_기본_liked_false로_직렬화되고_반영값을_따른다() throws Exception {
        CommentResponseDto dto = new CommentResponseDto(1L, 1L, "닉네임", false, null,
                null, null, "내용", 0, 0, null, null, 0L, 0L, false);
        assertLiked(dto, false);

        dto.updateLiked(true);
        assertLiked(dto, true);
    }

    @Test
    void CommentReplyResponseDto는_기본_liked_false로_직렬화되고_반영값을_따른다() throws Exception {
        CommentReplyResponseDto dto = CommentReplyResponseDto.builder().id(1L).build();
        assertLiked(dto, false);

        dto.updateLiked(true);
        assertLiked(dto, true);
    }

    @Test
    void CommentLikeResponseDto는_liked로_직렬화된다() throws Exception {
        assertLiked(CommentLikeResponseDto.builder().commentId(1L).likeCount(1).liked(true).message("좋아요 성공").build(), true);
        assertLiked(CommentLikeResponseDto.builder().commentId(1L).likeCount(0).liked(false).message("좋아요 취소").build(), false);
    }

    private void assertLiked(Object dto, boolean expected) throws Exception {
        JsonNode json = objectMapper.valueToTree(dto);
        assertThat(json.has("liked")).isTrue();
        assertThat(json.has("isLiked")).isFalse();
        assertThat(json.get("liked").isBoolean()).isTrue();
        assertThat(json.get("liked").asBoolean()).isEqualTo(expected);
    }
}
