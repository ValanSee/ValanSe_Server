package com.valanse.valanse.service.CommentLikeService;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.domain.Comment;
import com.valanse.valanse.domain.CommentGroup;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.Vote;
import com.valanse.valanse.domain.mapping.CommentLike;
import com.valanse.valanse.dto.Comment.CommentLikeResponseDto;
import com.valanse.valanse.repository.CommentLikeRepository;
import com.valanse.valanse.repository.CommentRepository;
import com.valanse.valanse.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentLikeServiceImplTest {

    @InjectMocks
    private CommentLikeServiceImpl commentLikeService;

    @Mock private CommentRepository commentRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private CommentLikeRepository commentLikeRepository;

    @BeforeEach
    void setupSecurityContext() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("1");
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    @Test
    @DisplayName("좋아요가 없는 댓글에 좋아요를 누르면 likeCount가 1 증가한다")
    void 좋아요_성공() {
        Member member = new Member();
        Vote vote = Vote.builder().id(1L).build();
        CommentGroup commentGroup = CommentGroup.builder().vote(vote).build();
        Comment comment = Comment.builder().content("댓글").commentGroup(commentGroup).likeCount(0).build();
        CommentLike commentLike = CommentLike.builder().comment(comment).user(member).build();

        when(memberRepository.findByIdAndDeletedAtIsNull(any())).thenReturn(Optional.of(member));
        when(commentRepository.findById(any())).thenReturn(Optional.of(comment));
        when(commentLikeRepository.findByUserIdAndCommentId(any(), any()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(commentLike));

        CommentLikeResponseDto result = commentLikeService.likeComment(1L, 1L);

        assertThat(result.getLikeCount()).isEqualTo(1);
        assertThat(result.getMessage()).isEqualTo("좋아요 성공");
    }

    @Test
    @DisplayName("이미 좋아요를 누른 댓글에 다시 누르면 취소되고 likeCount가 감소한다")
    void 좋아요_취소() {
        Member member = new Member();
        Vote vote = Vote.builder().id(1L).build();
        CommentGroup commentGroup = CommentGroup.builder().vote(vote).build();
        Comment comment = Comment.builder().content("댓글").commentGroup(commentGroup).likeCount(1).build();
        CommentLike commentLike = CommentLike.builder().comment(comment).user(member).build();

        when(memberRepository.findByIdAndDeletedAtIsNull(any())).thenReturn(Optional.of(member));
        when(commentRepository.findById(any())).thenReturn(Optional.of(comment));
        when(commentLikeRepository.findByUserIdAndCommentId(any(), any()))
                .thenReturn(Optional.of(commentLike))
                .thenReturn(Optional.empty());

        CommentLikeResponseDto result = commentLikeService.likeComment(1L, 1L);

        assertThat(result.getLikeCount()).isEqualTo(0);
        assertThat(result.getMessage()).isEqualTo("좋아요 취소");
    }

    @Test
    @DisplayName("존재하지 않는 회원이 좋아요를 시도하면 예외가 발생한다")
    void 좋아요_존재하지않는회원() {
        when(memberRepository.findByIdAndDeletedAtIsNull(any())).thenReturn(Optional.empty());

        assertThrows(Exception.class, () -> commentLikeService.likeComment(1L, 1L));
        verify(commentRepository, never()).findById(any());
    }

    @Test
    @DisplayName("존재하지 않는 댓글에 좋아요를 시도하면 예외가 발생한다")
    void 좋아요_존재하지않는댓글() {
        when(memberRepository.findByIdAndDeletedAtIsNull(any())).thenReturn(Optional.of(new Member()));
        when(commentRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(Exception.class, () -> commentLikeService.likeComment(1L, 999L));
    }

    @Test
    @DisplayName("다른 투표의 댓글에 좋아요를 누르면 예외가 발생하고 카운트를 변경하지 않는다")
    void 다른투표_commentId_좋아요실패() {
        Member member = new Member();
        Vote otherVote = Vote.builder().id(2L).build();
        CommentGroup otherCommentGroup = CommentGroup.builder().vote(otherVote).build();
        Comment comment = Comment.builder()
                .content("댓글")
                .commentGroup(otherCommentGroup)
                .likeCount(5)
                .build();

        when(memberRepository.findByIdAndDeletedAtIsNull(any())).thenReturn(Optional.of(member));
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));

        ApiException ex = assertThrows(ApiException.class,
                () -> commentLikeService.likeComment(1L, 10L));

        assertThat(ex.getStatus()).isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
        assertThat(comment.getLikeCount()).isEqualTo(5);
        verify(commentLikeRepository, never()).findByUserIdAndCommentId(any(), any());
        verify(commentLikeRepository, never()).save(any());
        verify(commentLikeRepository, never()).delete(any());
    }

    @Test
    @DisplayName("좋아요 저장 중 unique 제약 충돌이 발생하면 ApiException으로 변환한다")
    void 좋아요_unique제약충돌_ApiException() {
        Member member = new Member();
        Vote vote = Vote.builder().id(1L).build();
        CommentGroup commentGroup = CommentGroup.builder().vote(vote).build();
        Comment comment = Comment.builder()
                .content("댓글")
                .commentGroup(commentGroup)
                .likeCount(0)
                .build();

        when(memberRepository.findByIdAndDeletedAtIsNull(any())).thenReturn(Optional.of(member));
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));
        when(commentLikeRepository.findByUserIdAndCommentId(1L, 10L)).thenReturn(Optional.empty());
        when(commentLikeRepository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        ApiException ex = assertThrows(ApiException.class,
                () -> commentLikeService.likeComment(1L, 10L));

        assertThat(ex.getMessage()).isEqualTo("이미 좋아요한 댓글입니다.");
    }
}
