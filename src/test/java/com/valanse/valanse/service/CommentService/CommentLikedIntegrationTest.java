package com.valanse.valanse.service.CommentService;

import com.valanse.valanse.domain.Comment;
import com.valanse.valanse.domain.CommentGroup;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.Vote;
import com.valanse.valanse.domain.enums.Role;
import com.valanse.valanse.domain.enums.VoteCategory;
import com.valanse.valanse.domain.mapping.CommentLike;
import com.valanse.valanse.dto.Comment.CommentReplyResponseDto;
import com.valanse.valanse.dto.Comment.CommentResponseDto;
import com.valanse.valanse.repository.CommentGroupRepository;
import com.valanse.valanse.repository.CommentLikeRepository;
import com.valanse.valanse.repository.CommentRepository;
import com.valanse.valanse.repository.MemberRepository;
import com.valanse.valanse.repository.VoteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 댓글/대댓글 조회 응답의 liked 값이 실제 DB(comment_like)를 기준으로 채워지는지 검증합니다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CommentLikedIntegrationTest {
    @Autowired private CommentService commentService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private VoteRepository voteRepository;
    @Autowired private CommentGroupRepository commentGroupRepository;
    @Autowired private CommentRepository commentRepository;
    @Autowired private CommentLikeRepository commentLikeRepository;

    @Test
    void 좋아요한_댓글과_대댓글만_liked_true_비로그인은_모두_false() {
        Member writer = saveMember("liked-writer");
        Member viewer = saveMember("liked-viewer");
        Vote vote = voteRepository.saveAndFlush(Vote.builder()
                .member(writer).category(VoteCategory.ETC).title("투표").content("본문").build());
        CommentGroup group = commentGroupRepository.saveAndFlush(CommentGroup.builder().vote(vote).build());

        Comment likedComment = saveComment(writer, group, null, "좋아요한 댓글");
        Comment otherComment = saveComment(writer, group, null, "좋아요 안 한 댓글");
        Comment likedReply = saveComment(writer, group, likedComment, "좋아요한 대댓글");
        Comment otherReply = saveComment(writer, group, likedComment, "좋아요 안 한 대댓글");
        commentLikeRepository.saveAndFlush(CommentLike.builder().user(viewer).comment(likedComment).build());
        commentLikeRepository.saveAndFlush(CommentLike.builder().user(viewer).comment(likedReply).build());
        // 다른 사용자의 좋아요는 viewer의 liked에 영향을 주지 않아야 한다
        commentLikeRepository.saveAndFlush(CommentLike.builder().user(writer).comment(otherComment).build());

        assertThat(commentLikeRepository.findCommentIdsByUserIdAndCommentIdIn(viewer.getId(),
                List.of(likedComment.getId(), otherComment.getId(), likedReply.getId(), otherReply.getId())))
                .containsExactlyInAnyOrder(likedComment.getId(), likedReply.getId());

        Map<Long, Boolean> comments = commentService
                .getCommentsByVoteId(vote.getId(), "latest", PageRequest.of(0, 10), viewer.getId(), false)
                .getComments().stream()
                .collect(Collectors.toMap(CommentResponseDto::getCommentId, CommentResponseDto::isLiked));
        assertThat(comments).containsExactlyInAnyOrderEntriesOf(Map.of(
                likedComment.getId(), true,
                otherComment.getId(), false));

        Map<Long, Boolean> replies = commentService
                .getReplies(viewer, vote.getId(), likedComment.getId(), PageRequest.of(0, 10))
                .getReplies().stream()
                .collect(Collectors.toMap(CommentReplyResponseDto::getId, CommentReplyResponseDto::isLiked));
        assertThat(replies).containsExactlyInAnyOrderEntriesOf(Map.of(
                likedReply.getId(), true,
                otherReply.getId(), false));

        assertThat(commentService
                .getCommentsByVoteId(vote.getId(), "latest", PageRequest.of(0, 10), null, false)
                .getComments())
                .extracting(CommentResponseDto::isLiked).containsOnly(false);
        assertThat(commentService
                .getReplies(null, vote.getId(), likedComment.getId(), PageRequest.of(0, 10))
                .getReplies())
                .extracting(CommentReplyResponseDto::isLiked).containsOnly(false);
    }

    private Member saveMember(String prefix) {
        String socialId = prefix + "-" + System.nanoTime();
        return memberRepository.saveAndFlush(Member.builder()
                .socialId(socialId).email(socialId + "@example.com").name(prefix).role(Role.USER).build());
    }

    private Comment saveComment(Member member, CommentGroup group, Comment parent, String content) {
        return commentRepository.saveAndFlush(Comment.builder()
                .member(member).commentGroup(group).parent(parent)
                .content(content).likeCount(0).replyCount(0).build());
    }
}
