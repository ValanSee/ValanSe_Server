package com.valanse.valanse.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.valanse.valanse.domain.Comment;
import com.valanse.valanse.domain.CommentGroup;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.Vote;
import com.valanse.valanse.domain.enums.PinType;
import com.valanse.valanse.domain.enums.VoteCategory;
import com.valanse.valanse.dto.Comment.CommentReplyResponseDto;
import com.valanse.valanse.dto.Comment.CommentResponseDto;
import com.valanse.valanse.dto.Comment.MyCommentResponseDto;
import com.valanse.valanse.dto.Report.ReportedCommentResponse;
import com.valanse.valanse.dto.Vote.HotIssueVoteResponse;
import com.valanse.valanse.dto.Vote.VoteDetailResponse;
import com.valanse.valanse.dto.Vote.VoteListResponse;
import com.valanse.valanse.dto.Vote.VoteResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * isBot을 노출하는 모든 응답 DTO가 실제로 "isBot"이라는 JSON 필드명으로 직렬화되는지
 * 검증합니다. Boolean 필드 + Lombok @Getter 조합은 getIsBot()을 생성하는데, 이것이
 * "isBot"이 아니라 "bot"이나 "is_bot"으로 직렬화될 여지가 있어 코드만 보고는 확신할 수
 * 없으므로 직접 실행해 확인합니다. 일반 회원·봇 회원·탈퇴 회원 세 값도 함께 검증합니다.
 */
class IsBotJsonFieldNameTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void VoteResponseDto는_isBot으로_직렬화된다() throws Exception {
        assertIsBot(new VoteResponseDto(voteBy(botMember())), true);
        assertIsBot(new VoteResponseDto(voteBy(humanMember())), false);
        assertIsBot(new VoteResponseDto(voteBy(null)), false);
    }

    @Test
    void VoteDetailResponse는_isBot으로_직렬화된다() throws Exception {
        assertIsBot(VoteDetailResponse.builder().voteId(1L).isBot(true).build(), true);
        assertIsBot(VoteDetailResponse.builder().voteId(1L).isBot(false).build(), false);
    }

    @Test
    void HotIssueVoteResponse는_isBot으로_직렬화된다() throws Exception {
        assertIsBot(HotIssueVoteResponse.builder().voteId(1L).isBot(true).build(), true);
        assertIsBot(HotIssueVoteResponse.builder().voteId(1L).isBot(false).build(), false);
    }

    @Test
    void VoteListResponse_VoteDto는_isBot으로_직렬화된다() throws Exception {
        assertIsBot(VoteListResponse.VoteDto.builder().id(1L).isBot(true).build(), true);
        assertIsBot(VoteListResponse.VoteDto.builder().id(1L).isBot(false).build(), false);
    }

    @Test
    void CommentResponseDto는_isBot으로_직렬화된다() throws Exception {
        CommentResponseDto bot = new CommentResponseDto(1L, 1L, "닉네임", true, null,
                null, null, "내용", 0, 0, null, null, 0L, 0L, false);
        CommentResponseDto human = new CommentResponseDto(1L, 1L, "닉네임", false, null,
                null, null, "내용", 0, 0, null, null, 0L, 0L, false);

        assertIsBot(bot, true);
        assertIsBot(human, false);
    }

    @Test
    void CommentReplyResponseDto는_isBot으로_직렬화된다() throws Exception {
        assertIsBot(CommentReplyResponseDto.builder().id(1L).isBot(true).build(), true);
        assertIsBot(CommentReplyResponseDto.builder().id(1L).isBot(false).build(), false);
    }

    @Test
    void MyCommentResponseDto는_isBot으로_직렬화된다() throws Exception {
        assertIsBot(MyCommentResponseDto.fromEntity(commentBy(botMember())), true);
        assertIsBot(MyCommentResponseDto.fromEntity(commentBy(humanMember())), false);
    }

    @Test
    void ReportedCommentResponse는_isBot으로_직렬화된다() throws Exception {
        assertIsBot(new ReportedCommentResponse(commentBy(botMember())), true);
        assertIsBot(new ReportedCommentResponse(commentBy(humanMember())), false);
        assertIsBot(new ReportedCommentResponse(commentBy(null)), false);
    }

    private void assertIsBot(Object dto, boolean expected) throws Exception {
        String json = objectMapper.writeValueAsString(dto);
        assertThat(json).as("직렬화된 JSON: %s", json).contains("\"isBot\":" + expected);
        assertThat(json).doesNotContain("\"bot\":").doesNotContain("\"is_bot\":");
    }

    private Member botMember() {
        return Member.builder().id(1L).nickname("봇회원").isBot(true).build();
    }

    private Member humanMember() {
        return Member.builder().id(2L).nickname("일반회원").isBot(false).build();
    }

    private Vote voteBy(Member member) {
        Vote vote = Vote.builder()
                .id(1L)
                .title("title")
                .content("content")
                .category(VoteCategory.ALL)
                .totalVoteCount(0)
                .pinType(PinType.NONE)
                .member(member)
                .build();
        ReflectionTestUtils.setField(vote, "createdAt", LocalDateTime.now());
        return vote;
    }

    private Comment commentBy(Member member) {
        Vote vote = voteBy(member);
        CommentGroup commentGroup = CommentGroup.builder().id(10L).vote(vote).build();
        return Comment.builder()
                .id(1L)
                .content("댓글 내용")
                .likeCount(0)
                .replyCount(0)
                .member(member)
                .commentGroup(commentGroup)
                .build();
    }
}
