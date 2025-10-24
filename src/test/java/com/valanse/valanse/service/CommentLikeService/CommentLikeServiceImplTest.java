package com.valanse.valanse.service.CommentLikeService;

import com.valanse.valanse.domain.Comment;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.mapping.CommentLike;
import com.valanse.valanse.dto.Comment.CommentLikeResponseDto;
import com.valanse.valanse.repository.CommentLikeRepository;
import com.valanse.valanse.repository.CommentRepository;
import com.valanse.valanse.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentLikeServiceImplTest {
    @InjectMocks
    private CommentLikeServiceImpl commentLikeService;

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private CommentLikeRepository commentLikeRepository;

    // Security에서 userId를 뽑아오기 때문에 미리 설정
    @BeforeEach
    void setupSecurityContext() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("1"); // userId=1 가정
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }


    @Test
    public void 좋아요_test() {
        //given
        Member member = new Member();

        Comment comment = Comment.builder()
                .content("좋아요 없는 댓글")
                .likeCount(0)
                .build();

        CommentLike commentLike = CommentLike.builder()
                .comment(comment)
                .user(member)
                .build();
        // stub
        when(memberRepository.findByIdAndDeletedAtIsNull(any())).thenReturn(Optional.of(member));
        when(commentRepository.findById(any())).thenReturn(Optional.of(comment));
        // 첫번째 호출에서는 빈값, 두번째 호출에서는 commentLike 반환.
        when(commentLikeRepository.findByUserIdAndCommentId(any(), any()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(commentLike));

        // when
        CommentLikeResponseDto responseDto = commentLikeService.likeComment(1L, 1L);

        //then: 응답 dto 반환 값 확인
        assertThat(responseDto.getLikeCount()).isEqualTo(1);
        assertThat(responseDto.getMessage()).isEqualTo("좋아요 성공");
    }

    @Test
    void 좋아요_취소_test() {
        // when
        Member member = new Member();

        Comment comment = Comment.builder()
                .content("좋아요 누른 댓글")
                .likeCount(1)
                .build();

        CommentLike commentLike = CommentLike.builder()
                .comment(comment)
                .user(member)
                .build();

        // stub
        when(memberRepository.findByIdAndDeletedAtIsNull(any())).thenReturn(Optional.of(member));
        when(commentRepository.findById(any())).thenReturn(Optional.of(comment));
        when(commentLikeRepository.findByUserIdAndCommentId(any(), any()))
                .thenReturn(Optional.of(commentLike))
                .thenReturn(Optional.empty());

        //when
        CommentLikeResponseDto responseDto = commentLikeService.likeComment(1L, 1L);

        //then: 응답 dto 반환 값 확인
        assertThat(responseDto.getLikeCount()).isEqualTo(0);
        assertThat(responseDto.getMessage()).isEqualTo("좋아요 취소");
    }
  
}