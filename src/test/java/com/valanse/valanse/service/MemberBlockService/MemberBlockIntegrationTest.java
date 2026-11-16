package com.valanse.valanse.service.MemberBlockService;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.domain.Comment;
import com.valanse.valanse.domain.CommentGroup;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.MemberProfile;
import com.valanse.valanse.domain.Vote;
import com.valanse.valanse.domain.VoteOption;
import com.valanse.valanse.domain.enums.Role;
import com.valanse.valanse.domain.enums.VoteCategory;
import com.valanse.valanse.domain.enums.VoteLabel;
import com.valanse.valanse.dto.Comment.CommentReplyResponseDto;
import com.valanse.valanse.dto.Comment.CommentResponseDto;
import com.valanse.valanse.dto.MemberBlock.MemberBlockListResponse;
import com.valanse.valanse.dto.Vote.TrendingVoteItemResponse;
import com.valanse.valanse.dto.Vote.VoteListResponse;
import com.valanse.valanse.repository.CommentGroupRepository;
import com.valanse.valanse.repository.CommentRepository;
import com.valanse.valanse.repository.MemberBlockRepository;
import com.valanse.valanse.repository.MemberProfileRepository;
import com.valanse.valanse.repository.MemberRepository;
import com.valanse.valanse.repository.VoteOptionRepository;
import com.valanse.valanse.repository.VoteRepository;
import com.valanse.valanse.service.CommentService.CommentService;
import com.valanse.valanse.service.VoteService.VoteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 회원 차단/차단 해제/목록 조회와, 차단한 작성자의 콘텐츠가 목록 조회에서 제외되는지 실제 DB(H2)로 검증합니다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MemberBlockIntegrationTest {

    @Autowired private MemberBlockService memberBlockService;
    @Autowired private CommentService commentService;
    @Autowired private VoteService voteService;
    @Autowired private MemberBlockRepository memberBlockRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private MemberProfileRepository memberProfileRepository;
    @Autowired private VoteRepository voteRepository;
    @Autowired private VoteOptionRepository voteOptionRepository;
    @Autowired private CommentGroupRepository commentGroupRepository;
    @Autowired private CommentRepository commentRepository;

    @Test
    @DisplayName("차단은 멱등이고, 차단 해제도 멱등이며, 목록은 최근 차단순으로 닉네임과 함께 반환된다")
    void blockUnblockAndList() {
        Member me = saveMember("block-me", "나");
        Member first = saveMember("block-first", "먼저차단");
        Member second = saveMember("block-second", null); // 프로필 없음

        memberBlockService.block(me.getId(), first.getId());
        memberBlockService.block(me.getId(), first.getId()); // 두 번 차단해도 성공
        memberBlockService.block(me.getId(), second.getId());

        assertThat(memberBlockRepository.findAllByBlockerIdWithBlocked(me.getId())).hasSize(2);

        MemberBlockListResponse blocks = memberBlockService.getBlocks(me.getId());
        assertThat(blocks.blocks())
                .extracting(MemberBlockListResponse.BlockedMember::memberId)
                .containsExactly(second.getId(), first.getId());
        assertThat(blocks.blocks().get(0).nickname()).isNull();
        assertThat(blocks.blocks().get(1).nickname()).isEqualTo("먼저차단");
        assertThat(blocks.blocks().get(0).blockedAt()).isNotNull();

        memberBlockService.unblock(me.getId(), first.getId());
        memberBlockService.unblock(me.getId(), first.getId()); // 이미 해제돼 있어도 성공

        assertThat(memberBlockService.getBlocks(me.getId()).blocks())
                .extracting(MemberBlockListResponse.BlockedMember::memberId)
                .containsExactly(second.getId());
    }

    @Test
    @DisplayName("자기 자신 차단은 400, 존재하지 않거나 탈퇴한 회원 차단은 404")
    void blockValidation() {
        Member me = saveMember("block-validate-me", "나");
        Member withdrawn = saveMember("block-validate-withdrawn", "탈퇴자");
        withdrawn.softDelete();
        memberRepository.saveAndFlush(withdrawn);

        assertThatThrownBy(() -> memberBlockService.block(me.getId(), me.getId()))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> memberBlockService.block(me.getId(), Long.MAX_VALUE))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> memberBlockService.block(me.getId(), withdrawn.getId()))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("차단한 작성자의 댓글·대댓글은 목록과 대표 댓글에서 제외되고, 비로그인/다른 회원에게는 그대로 보인다")
    void blockedAuthorCommentsAreExcluded() {
        Member viewer = saveMember("block-comment-viewer", "보는사람");
        Member abuser = saveMember("block-comment-abuser", "악플러");
        Member normal = saveMember("block-comment-normal", "일반인");
        Vote vote = saveVote(normal);
        CommentGroup group = commentGroupRepository.saveAndFlush(CommentGroup.builder().vote(vote).build());

        Comment abuserComment = saveComment(abuser, group, null, "악플", 100);
        Comment normalComment = saveComment(normal, group, null, "정상 댓글", 1);
        Comment abuserReply = saveComment(abuser, group, normalComment, "악플 대댓글", 0);
        Comment normalReply = saveComment(normal, group, normalComment, "정상 대댓글", 0);

        // 차단 전: 모두 보이고 authorId가 채워진다
        List<CommentResponseDto> before = commentService
                .getCommentsByVoteId(vote.getId(), "latest", PageRequest.of(0, 10), viewer.getId(), false)
                .getComments();
        assertThat(before).extracting(CommentResponseDto::getCommentId)
                .containsExactlyInAnyOrder(abuserComment.getId(), normalComment.getId());
        assertThat(before).filteredOn(c -> c.getCommentId().equals(abuserComment.getId()))
                .extracting(CommentResponseDto::getAuthorId).containsExactly(abuser.getId());
        assertThat(commentService.getBestCommentByVoteId(vote.getId(), viewer.getId()).content()).isEqualTo("악플");

        memberBlockService.block(viewer.getId(), abuser.getId());

        // 차단 후: 차단한 회원에게는 제외
        assertThat(commentService
                .getCommentsByVoteId(vote.getId(), "popular", PageRequest.of(0, 10), viewer.getId(), false)
                .getComments())
                .extracting(CommentResponseDto::getCommentId)
                .containsExactly(normalComment.getId());
        List<CommentReplyResponseDto> replies = commentService
                .getReplies(viewer, vote.getId(), normalComment.getId(), PageRequest.of(0, 10))
                .getReplies();
        assertThat(replies).extracting(CommentReplyResponseDto::getId).containsExactly(normalReply.getId());
        assertThat(replies).extracting(CommentReplyResponseDto::getAuthorId).containsExactly(normal.getId());
        assertThat(commentService.getBestCommentByVoteId(vote.getId(), viewer.getId()).content())
                .isEqualTo("정상 댓글");

        // 차단 첫 페이지 크기 1에서도 차단 댓글이 자리를 차지하지 않는다 (페이지 전에 필터링)
        assertThat(commentService
                .getCommentsByVoteId(vote.getId(), "popular", PageRequest.of(0, 1), viewer.getId(), false)
                .getComments())
                .extracting(CommentResponseDto::getCommentId)
                .containsExactly(normalComment.getId());

        // 비로그인에게는 그대로 보인다
        assertThat(commentService
                .getCommentsByVoteId(vote.getId(), "latest", PageRequest.of(0, 10), null, false)
                .getComments())
                .extracting(CommentResponseDto::getCommentId)
                .containsExactlyInAnyOrder(abuserComment.getId(), normalComment.getId());
        assertThat(commentService.getReplies(null, vote.getId(), normalComment.getId(), PageRequest.of(0, 10))
                .getReplies())
                .extracting(CommentReplyResponseDto::getId)
                .containsExactlyInAnyOrder(abuserReply.getId(), normalReply.getId());
        assertThat(commentService.getBestCommentByVoteId(vote.getId()).content()).isEqualTo("악플");
    }

    @Test
    @DisplayName("차단한 작성자의 투표는 투표 목록과 트렌딩에서 제외되지만 상세 조회는 가능하다")
    void blockedAuthorVotesAreExcluded() {
        Member viewer = saveMember("block-vote-viewer", "보는사람");
        Member abuser = saveMember("block-vote-abuser", "어뷰저");
        Member normal = saveMember("block-vote-normal", "일반인");
        Vote normalVote = saveVote(normal);
        Vote abuserVote = saveVote(abuser);
        // 트렌딩 후보가 되도록 두 투표 모두 반응(댓글)을 넉넉히 만든다
        addReactions(normalVote, normal, 20);
        addReactions(abuserVote, normal, 30);

        memberBlockService.block(viewer.getId(), abuser.getId());

        List<VoteListResponse.VoteDto> anonymousList = voteService
                .getVotesByCategoryAndSort(null, "ALL", "latest", null, 10, null).getVotes();
        assertThat(anonymousList).extracting(VoteListResponse.VoteDto::getId)
                .contains(abuserVote.getId(), normalVote.getId());
        assertThat(anonymousList).filteredOn(v -> v.getId().equals(abuserVote.getId()))
                .extracting(VoteListResponse.VoteDto::getAuthorId).containsExactly(abuser.getId());

        List<VoteListResponse.VoteDto> viewerList = voteService
                .getVotesByCategoryAndSort(viewer, "ALL", "latest", null, 10, null).getVotes();
        assertThat(viewerList).extracting(VoteListResponse.VoteDto::getId)
                .contains(normalVote.getId())
                .doesNotContain(abuserVote.getId());

        List<TrendingVoteItemResponse> anonymousTrending = voteService.getTrendingVotes(1, null).getVotes();
        assertThat(anonymousTrending).extracting(TrendingVoteItemResponse::getVoteId).contains(abuserVote.getId());
        assertThat(anonymousTrending).filteredOn(v -> v.getVoteId().equals(abuserVote.getId()))
                .extracting(TrendingVoteItemResponse::getAuthorId).containsExactly(abuser.getId());

        List<TrendingVoteItemResponse> viewerTrending = voteService.getTrendingVotes(1, viewer.getId()).getVotes();
        assertThat(viewerTrending).extracting(TrendingVoteItemResponse::getVoteId)
                .contains(normalVote.getId())
                .doesNotContain(abuserVote.getId());

        // 공유 링크로 들어오는 상세 조회는 차단 여부와 상관없이 가능하고 authorId를 내려준다
        assertThat(voteService.getVoteDetailById(abuserVote.getId()).getAuthorId()).isEqualTo(abuser.getId());
    }

    private Member saveMember(String prefix, String nickname) {
        String socialId = prefix + "-" + System.nanoTime();
        Member member = memberRepository.saveAndFlush(Member.builder()
                .socialId(socialId).email(socialId + "@example.com").name(prefix).role(Role.USER).build());
        if (nickname != null) {
            memberProfileRepository.saveAndFlush(MemberProfile.builder()
                    .member(member).nickname(nickname).point(0L).build());
        }
        return member;
    }

    private Vote saveVote(Member author) {
        Vote vote = voteRepository.saveAndFlush(Vote.builder()
                .member(author).category(VoteCategory.ETC).title("투표-" + author.getName()).content("본문")
                .totalVoteCount(0).build());
        voteOptionRepository.saveAndFlush(VoteOption.builder()
                .vote(vote).content("A").label(VoteLabel.A).voteCount(0).build());
        voteOptionRepository.saveAndFlush(VoteOption.builder()
                .vote(vote).content("B").label(VoteLabel.B).voteCount(0).build());
        return vote;
    }

    private void addReactions(Vote vote, Member commenter, int count) {
        CommentGroup group = commentGroupRepository.saveAndFlush(CommentGroup.builder().vote(vote).build());
        for (int i = 0; i < count; i++) {
            saveComment(commenter, group, null, "반응 " + i, 0);
        }
    }

    private Comment saveComment(Member member, CommentGroup group, Comment parent, String content, int likeCount) {
        return commentRepository.saveAndFlush(Comment.builder()
                .member(member).commentGroup(group).parent(parent)
                .content(content).likeCount(likeCount).replyCount(0).build());
    }
}
