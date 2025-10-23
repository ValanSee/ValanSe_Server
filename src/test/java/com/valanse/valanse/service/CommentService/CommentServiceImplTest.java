package com.valanse.valanse.service.CommentService;

import com.valanse.valanse.domain.Comment;
import com.valanse.valanse.domain.CommentGroup;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.Vote;
import com.valanse.valanse.domain.enums.Role;
import com.valanse.valanse.dto.Comment.CommentPostRequest;
import com.valanse.valanse.repository.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
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
    @BeforeEach
    void setUp() {
        member = Member.builder()
                .id(1L)
                .email("test@email.com")
                .nickname("테스트")
                .name("test")
                .role(Role.USER)
                .build();
        vote = Vote.builder()
                .id(1L)
                .build();
    }

    @Test
    void 새로운댓글생성_test() {
        //given

        CommentGroup newGroup = CommentGroup.builder()
                .vote(vote)
                .totalCommentCount(0)
                .build();

        CommentPostRequest request = CommentPostRequest.builder()
                .content("새로운 댓글")
                .parentId(null)
                .build();

        Comment newComment = Comment.builder()
                .content(request.getContent())
                .member(member)
                .commentGroup(newGroup)
                .parent(null)
                .likeCount(0)
                .replyCount(0)
                .deletedAt(null)
                .build();

        //stub
        when(voteRepository.findById(1L))
                .thenReturn(Optional.of(vote));
        when(memberRepository.findById(1L))
                .thenReturn(Optional.of(member));
        when(commentGroupRepository.findByVoteId(1L))
                .thenReturn(Optional.empty());
        when(commentGroupRepository.save(any())).
                thenReturn(newGroup);
        when(commentRepository.save(any())).
                thenReturn(newComment);

        //when
        commentService.createComment(1L,1L,request);

        //then
        ArgumentCaptor<Comment> commentCaptor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository,times(1)).save(commentCaptor.capture());

        Comment savedComment = commentCaptor.getValue();
        assertThat("새로운 댓글").isEqualTo(savedComment.getContent());
        assertNull(savedComment.getParent());

        ArgumentCaptor<CommentGroup> commentGroupCaptor = ArgumentCaptor.forClass(CommentGroup.class);
        verify(commentGroupRepository,times(2)).save(commentGroupCaptor.capture());

        CommentGroup commentGroup = commentGroupCaptor.getValue();
        assertThat(commentGroup.getTotalCommentCount()).isEqualTo(1L);
        assertThat(commentGroup.getVote().getId()).isEqualTo(1L);

        verify(commentRepository, never()).findById(any());
    }

    @Test
    void 대댓글생성_test() {
        CommentGroup newGroup = CommentGroup.builder()
                .vote(vote)
                .totalCommentCount(1)
                .build();

        CommentPostRequest newRequest = CommentPostRequest.builder()
                .content("새로운 댓글")
                .parentId(1L)
                .build();

        CommentPostRequest parentRequest = CommentPostRequest.builder()
                .content("부모 댓글")
                .parentId(null)
                .build();

        Comment parentComment = Comment.builder()
                .content(newRequest.getContent())
                .member(member)
                .commentGroup(newGroup)
                .parent(null)
                .likeCount(0)
                .replyCount(0)
                .build();

        Comment newComment = Comment.builder()
                .content(newRequest.getContent())
                .member(member)
                .commentGroup(newGroup)
                .parent(parentComment)
                .likeCount(0)
                .replyCount(0)
                .deletedAt(null)
                .build();

        // stub
        when(voteRepository.findById(1L))
                .thenReturn(Optional.of(vote));
        when(memberRepository.findById(1L))
                .thenReturn(Optional.of(member));
        when(commentGroupRepository.findByVoteId(1L))
                .thenReturn(Optional.of(newGroup));
        when(commentRepository.findById(any()))
                .thenReturn(Optional.of(parentComment));
        when(commentGroupRepository.save(any())).
                thenReturn(newGroup);
        when(commentRepository.save(any())).
                thenReturn(newComment);


        //when
        commentService.createComment(1L,1L,newRequest);

        // then
        ArgumentCaptor<Comment> commentCaptor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository,times(1)).save(commentCaptor.capture());

        Comment savedComment = commentCaptor.getValue();
        assertThat("새로운 댓글").isEqualTo(savedComment.getContent());
        assertThat(savedComment.getParent()).isEqualTo(parentComment);

        ArgumentCaptor<CommentGroup> commentGroupCaptor = ArgumentCaptor.forClass(CommentGroup.class);
        verify(commentGroupRepository,times(1)).save(commentGroupCaptor.capture());

        CommentGroup commentGroup = commentGroupCaptor.getValue();
        assertThat(commentGroup.getTotalCommentCount()).isEqualTo(2L);
        assertThat(commentGroup.getVote().getId()).isEqualTo(1L);


        verify(commentRepository, times(1)).findById(any());
    }

    @Test
    void 댓글삭제_test() {
        // given
        CommentGroup newGroup = CommentGroup.builder()
                .vote(vote)
                .totalCommentCount(1)
                .build();


        Comment comment = Comment.builder()
                .content("댓글")
                .member(member)
                .commentGroup(newGroup)
                .parent(null)
                .likeCount(0)
                .replyCount(0)
                .deletedAt(null)
                .build();

        // stub
        when(commentRepository.findById(1L))
                .thenReturn(Optional.of(comment));
        when(commentRepository.save(any()))
                .thenReturn(comment);

        //when
        commentService.deleteMyComment(member, 1L);

        //then
        ArgumentCaptor<Comment> commentCaptor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository,times(1)).save(commentCaptor.capture());

        Comment deleted = commentCaptor.getValue();
        assertThat(deleted.getDeletedAt()).isNotNull();

    }

}