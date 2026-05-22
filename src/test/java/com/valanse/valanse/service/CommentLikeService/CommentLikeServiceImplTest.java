package com.valanse.valanse.service.CommentLikeService;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.domain.Comment;
import com.valanse.valanse.domain.Member;
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
        Comment comment = Comment.builder().content("댓글").likeCount(0).build();
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
        Comment comment = Comment.builder().content("댓글").likeCount(1).build();
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
}
