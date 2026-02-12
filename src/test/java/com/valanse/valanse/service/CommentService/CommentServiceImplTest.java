package com.valanse.valanse.service.CommentService;

import com.valanse.valanse.domain.*;
import com.valanse.valanse.domain.enums.Role;
import com.valanse.valanse.dto.Comment.BestCommentResponseDto;
import com.valanse.valanse.dto.Comment.CommentPostRequest;
import com.valanse.valanse.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {
    @InjectMocks
    private CommentServiceImpl commentService;

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private VoteRepository voteRepository;
    @Mock
    private CommentGroupRepository commentGroupRepository;
    @Mock
    private MemberProfileRepository memberProfileRepository;

    private Member member;
    private Vote vote;
    private CommentGroup commentGroup;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .id(1L)
                .email("test@email.com")
                .name("test")
                .role(Role.USER)
                .build();

        vote = Vote.builder()
                .id(1L)
                .member(member)
                .build();

        commentGroup = CommentGroup.builder()
                .id(1L)
                .vote(vote)
                .totalCommentCount(0)
                .build();
    }

    // ==================== 댓글 생성 테스트 ====================

    @Test
    @DisplayName("부모 댓글 생성 시 totalCommentCount만 증가")
    void 부모댓글생성_test() {
        // given
        CommentPostRequest request = CommentPostRequest.builder()
                .content("새로운 부모 댓글")
                .parentId(null) // 부모 댓글
                .build();

        Comment newComment = Comment.builder()
                .id(10L)
                .content(request.getContent())
                .member(member)
                .commentGroup(commentGroup)
                .parent(null)
                .likeCount(0)
                .replyCount(0)
                .deletedAt(null)
                .build();

        // stub
        when(voteRepository.findById(1L)).thenReturn(Optional.of(vote));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(commentGroupRepository.findByVoteId(1L)).thenReturn(Optional.of(commentGroup));
        when(commentRepository.save(any())).thenReturn(newComment);

        // when
        Long commentId = commentService.createComment(1L, 1L, request);

        // then
        assertThat(commentId).isEqualTo(10L);

        // totalCommentCount가 1 증가했는지 확인
        ArgumentCaptor<CommentGroup> groupCaptor = ArgumentCaptor.forClass(CommentGroup.class);
        verify(commentGroupRepository, times(1)).save(groupCaptor.capture());
        assertThat(groupCaptor.getValue().getTotalCommentCount()).isEqualTo(1);

        // Comment 저장 확인
        verify(commentRepository, times(1)).save(any(Comment.class));
    }

    @Test
    @DisplayName("대댓글 생성 시 totalCommentCount 증가 없이 부모의 replyCount만 증가")
    void 대댓글생성_test() {
        // given
        Comment parentComment = Comment.builder()
                .id(10L)
                .content("부모 댓글")
                .member(member)
                .commentGroup(commentGroup)
                .parent(null)
                .likeCount(0)
                .replyCount(0)
                .build();

        CommentPostRequest request = CommentPostRequest.builder()
                .content("대댓글")
                .parentId(10L) // 대댓글
                .build();

        Comment replyComment = Comment.builder()
                .id(11L)
                .content(request.getContent())
                .member(member)
                .commentGroup(commentGroup)
                .parent(parentComment)
                .likeCount(0)
                .replyCount(0)
                .deletedAt(null)
                .build();

        // stub
        when(voteRepository.findById(1L)).thenReturn(Optional.of(vote));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(commentGroupRepository.findByVoteId(1L)).thenReturn(Optional.of(commentGroup));
        when(commentRepository.findById(10L)).thenReturn(Optional.of(parentComment));
        when(commentRepository.save(any())).thenReturn(replyComment);

        // when
        Long commentId = commentService.createComment(1L, 1L, request);

        // then
        assertThat(commentId).isEqualTo(11L);

        // 부모 댓글의 replyCount가 증가했는지 확인
        assertThat(parentComment.getReplyCount()).isEqualTo(1);

        // totalCommentCount는 증가하지 않아야 함 (대댓글이므로)
        verify(commentGroupRepository, never()).save(commentGroup);

        // 부모 댓글 저장 확인
        verify(commentRepository, times(2)).save(any(Comment.class)); // parent 저장 + reply 저장
    }

    @Test
    @DisplayName("CommentGroup이 없을 때 자동 생성 후 댓글 작성")
    void CommentGroup없을때_자동생성_test() {
        // given
        CommentPostRequest request = CommentPostRequest.builder()
                .content("첫 번째 댓글")
                .parentId(null)
                .build();

        CommentGroup newGroup = CommentGroup.builder()
                .vote(vote)
                .totalCommentCount(0)
                .build();

        Comment newComment = Comment.builder()
                .content(request.getContent())
                .member(member)
                .commentGroup(newGroup)
                .build();

        // stub
        when(voteRepository.findById(1L)).thenReturn(Optional.of(vote));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(commentGroupRepository.findByVoteId(1L)).thenReturn(Optional.empty()); // CommentGroup 없음
        when(commentGroupRepository.save(any())).thenReturn(newGroup);
        when(commentRepository.save(any())).thenReturn(newComment);

        // when
        commentService.createComment(1L, 1L, request);

        // then
        // CommentGroup이 생성되었는지 확인
        verify(commentGroupRepository, times(2)).save(any(CommentGroup.class)); // 생성 + totalCount 증가
    }

    // ==================== 댓글 삭제 테스트 ====================

    @Test
    @DisplayName("부모 댓글 삭제 시 totalCommentCount 감소")
    void 부모댓글삭제_test() {
        // given
        commentGroup.setTotalCommentCount(5); // 기존에 5개의 댓글

        Comment comment = Comment.builder()
                .id(10L)
                .content("부모 댓글")
                .member(member)
                .commentGroup(commentGroup)
                .parent(null) // 부모 댓글
                .likeCount(0)
                .replyCount(0)
                .deletedAt(null)
                .build();

        // stub
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any())).thenReturn(comment);

        // when
        commentService.deleteMyComment(member, 10L);

        // then
        // soft delete 확인
        assertThat(comment.getDeletedAt()).isNotNull();

        // totalCommentCount 감소 확인
        ArgumentCaptor<CommentGroup> groupCaptor = ArgumentCaptor.forClass(CommentGroup.class);
        verify(commentGroupRepository, times(1)).save(groupCaptor.capture());
        assertThat(groupCaptor.getValue().getTotalCommentCount()).isEqualTo(4);
    }

    @Test
    @DisplayName("대댓글 삭제 시 부모 댓글의 replyCount 감소")
    void 대댓글삭제_test() {
        // given
        Comment parentComment = Comment.builder()
                .id(10L)
                .content("부모 댓글")
                .member(member)
                .commentGroup(commentGroup)
                .parent(null)
                .replyCount(3) // 기존 대댓글 3개
                .build();

        Comment replyComment = Comment.builder()
                .id(11L)
                .content("대댓글")
                .member(member)
                .commentGroup(commentGroup)
                .parent(parentComment) // 대댓글
                .likeCount(0)
                .replyCount(0)
                .deletedAt(null)
                .build();

        // stub
        when(commentRepository.findById(11L)).thenReturn(Optional.of(replyComment));
        when(commentRepository.save(any())).thenReturn(replyComment);

        // when
        commentService.deleteMyComment(member, 11L);

        // then
        // soft delete 확인
        assertThat(replyComment.getDeletedAt()).isNotNull();

        // 부모 댓글의 replyCount 감소 확인
        assertThat(parentComment.getReplyCount()).isEqualTo(2);
        verify(commentRepository, times(2)).save(any(Comment.class)); // reply 저장 + parent 저장
    }

    @Test
    @DisplayName("관리자가 다른 사용자의 댓글 삭제 가능")
    void 관리자댓글삭제_test() {
        // given
        Member admin = Member.builder()
                .id(999L)
                .role(Role.ADMIN)
                .build();

        Comment comment = Comment.builder()
                .id(10L)
                .content("다른 사용자 댓글")
                .member(member) // 작성자는 일반 사용자
                .commentGroup(commentGroup)
                .parent(null)
                .deletedAt(null)
                .build();

        // stub
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any())).thenReturn(comment);

        // when
        commentService.deleteMyComment(admin, 10L);

        // then
        assertThat(comment.getDeletedAt()).isNotNull();
        verify(commentRepository, times(1)).save(any(Comment.class));
    }

    @Test
    @DisplayName("일반 사용자는 다른 사용자의 댓글 삭제 불가")
    void 권한없는사용자_댓글삭제실패_test() {
        // given
        Member otherUser = Member.builder()
                .id(999L)
                .role(Role.USER)
                .build();

        Comment comment = Comment.builder()
                .id(10L)
                .content("다른 사용자 댓글")
                .member(member) // 작성자는 member (id=1)
                .deletedAt(null)
                .build();

        // stub
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));

        //when
        ApiException apiException = assertThrows(ApiException.class, () -> commentService.deleteMyComment(admin, 1L));

        //then
        assertThat(apiException.getMessage()).isEqualTo("삭제 권한이 없습니다.");

        // when
        commentService.deleteMyComment(member, 999L);

        // then
        // ifPresentOrElse의 orElse 분기로 가서 로그만 출력
        verify(commentRepository, never()).save(any());
        verify(commentGroupRepository, never()).save(any());
    }

    // ==================== 복합 시나리오 테스트 ====================

    @Test
    @DisplayName("부모 댓글과 대댓글 2개를 작성 후 대댓글 1개 삭제")
    void 복합시나리오_댓글생성후삭제_test() {
        // given
        commentGroup.setTotalCommentCount(0);

        // 1. 부모 댓글 생성
        Comment parentComment = Comment.builder()
                .id(10L)
                .content("부모 댓글")
                .member(member)
                .commentGroup(commentGroup)
                .parent(null)
                .replyCount(0)
                .build();

        CommentPostRequest parentRequest = CommentPostRequest.builder()
                .content("부모 댓글")
                .parentId(null)
                .build();

        when(voteRepository.findById(1L)).thenReturn(Optional.of(vote));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(commentGroupRepository.findByVoteId(1L)).thenReturn(Optional.of(commentGroup));
        when(commentRepository.save(any())).thenReturn(parentComment);

        // when 1: 부모 댓글 생성
        commentService.createComment(1L, 1L, parentRequest);

        // then 1: totalCommentCount = 1
        assertThat(commentGroup.getTotalCommentCount()).isEqualTo(1);

        // 2. 대댓글 2개 생성
        when(commentRepository.findById(10L)).thenReturn(Optional.of(parentComment));
        CommentPostRequest replyRequest = CommentPostRequest.builder()
                .content("대댓글")
                .parentId(10L)
                .build();

        // when 2: 대댓글 2개 생성
        commentService.createComment(1L, 1L, replyRequest);
        commentService.createComment(1L, 1L, replyRequest);

        // then 2: replyCount = 2, totalCommentCount = 1 (변화 없음)
        assertThat(parentComment.getReplyCount()).isEqualTo(2);
        assertThat(commentGroup.getTotalCommentCount()).isEqualTo(1);

        // 3. 대댓글 1개 삭제
        Comment replyToDelete = Comment.builder()
                .id(11L)
                .member(member)
                .commentGroup(commentGroup)
                .parent(parentComment)
                .build();

        when(commentRepository.findById(11L)).thenReturn(Optional.of(replyToDelete));

        // when 3: 대댓글 삭제
        commentService.deleteMyComment(member, 11L);

        // then 3: replyCount = 1, totalCommentCount = 1 (변화 없음)
        assertThat(parentComment.getReplyCount()).isEqualTo(1);
        assertThat(commentGroup.getTotalCommentCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("getBestCommentByVoteId - actualCommentCount 사용 확인")
    void getBestComment_actualCount_test() {
        // given
        commentGroup.setTotalCommentCount(10); // DB에 저장된 값 (오래된 값)

        Comment bestComment = Comment.builder()
                .id(1L)
                .content("인기 댓글")
                .likeCount(100)
                .build();

        when(commentGroupRepository.findByVoteId(1L)).thenReturn(Optional.of(commentGroup));
        when(commentRepository.countActiveCommentsByVoteId(1L)).thenReturn(7L); // 실제 활성 댓글 수
        when(commentRepository.findMostLikedCommentByVoteId(1L)).thenReturn(Optional.of(bestComment));

        // when
        BestCommentResponseDto response = commentService.getBestCommentByVoteId(1L);

        // then
        assertThat(response.totalCommentCount()).isEqualTo(7);
        assertThat(response.content()).isEqualTo("인기 댓글");
    }
}