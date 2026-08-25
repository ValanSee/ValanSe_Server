package com.valanse.valanse.dto.Report;

import com.valanse.valanse.domain.Comment;
import com.valanse.valanse.domain.CommentGroup;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.Vote;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReportedCommentResponseTest {

    @Test
    void 일반_회원_댓글은_isBot이_false다() {
        Member member = Member.builder().id(1L).nickname("일반회원").isBot(false).build();
        Comment comment = commentBy(member);

        ReportedCommentResponse response = new ReportedCommentResponse(comment);

        assertThat(response.getNickname()).isEqualTo("일반회원");
        assertThat(response.getIsBot()).isFalse();
    }

    @Test
    void 봇_회원_댓글은_isBot이_true다() {
        Member bot = Member.builder().id(2L).nickname("봇회원").isBot(true).build();
        Comment comment = commentBy(bot);

        ReportedCommentResponse response = new ReportedCommentResponse(comment);

        assertThat(response.getNickname()).isEqualTo("봇회원");
        assertThat(response.getIsBot()).isTrue();
    }

    @Test
    void 탈퇴_회원_댓글은_isBot이_false다() {
        Comment comment = commentBy(null);

        ReportedCommentResponse response = new ReportedCommentResponse(comment);

        assertThat(response.getNickname()).isEqualTo("탈퇴한 사용자");
        assertThat(response.getIsBot()).isFalse();
    }

    private Comment commentBy(Member member) {
        Vote vote = Vote.builder().id(100L).build();
        CommentGroup commentGroup = CommentGroup.builder().id(10L).vote(vote).build();
        return Comment.builder()
                .id(1L)
                .content("신고된 댓글")
                .likeCount(0)
                .replyCount(0)
                .member(member)
                .commentGroup(commentGroup)
                .build();
    }
}
