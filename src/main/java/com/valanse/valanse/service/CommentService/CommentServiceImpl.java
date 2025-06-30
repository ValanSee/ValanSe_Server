package com.valanse.valanse.service.CommentService;

import com.valanse.valanse.domain.*;
import com.valanse.valanse.domain.enums.VoteLabel;
import com.valanse.valanse.dto.Comment.*;
import com.valanse.valanse.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final MemberRepository memberRepository;
    private final VoteRepository voteRepository;
    private final CommentGroupRepository commentGroupRepository;
    private final CommentRepository commentRepository;
    private final MemberProfileRepository memberProfileRepository;

    @Override
    @Transactional
    public Long createComment(Long voteId, Long userId, CommentPostRequest request) {
        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new IllegalArgumentException("Vote not found"));
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        // 1. CommentGroup 찾기 (없으면 생성)
        CommentGroup commentGroup = commentGroupRepository.findByVoteId(voteId)
                .orElseGet(() -> {
                    CommentGroup newGroup = CommentGroup.builder()
                            .vote(vote)
                            .totalCommentCount(0)
                            .build();
                    return commentGroupRepository.save(newGroup);
                });

        // 2. 부모 댓글이 있을 경우 가져오기
        Comment parent = null;
        if (request.getParentId() != null) {
            parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent comment not found"));
            parent.updateReplyCount(parent.getReplyCount() + 1); // replyCount 증가
        }

        // 3. 댓글 저장
        Comment comment = Comment.builder()
                .content(request.getContent())
                .member(member)
                .commentGroup(commentGroup)
                .parent(parent)
                .likeCount(0)
                .replyCount(0)
                .isDeleted(false)
                .build();
        commentGroup.setTotalCommentCount(commentGroup.getTotalCommentCount() + 1); // 총 댓글 수 증가
        commentGroupRepository.save(commentGroup);

        return commentRepository.save(comment).getId();
    }

    @Override
    public PagedCommentResponse getCommentsByVoteId(Long voteId, String sort, Pageable pageable) {
        Slice<CommentResponseDto> slice = commentRepository.findCommentsByVoteIdSlice(voteId, sort, pageable);
        return PagedCommentResponse.builder() // 인덱스 , 쿼리
                .comments(slice.getContent())
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .hasNext(slice.hasNext())
                .build();
    }

    @Override
    public BestCommentResponseDto getBestCommentByVoteId(Long voteId) {
        CommentGroup group = commentGroupRepository.findByVoteId(voteId)
                .orElseThrow(() -> new IllegalArgumentException("comment group not found"));

        return commentRepository.findMostLikedCommentByVoteId(voteId)
                .map(comment -> BestCommentResponseDto.builder()
                        .totalCommentCount(group.getTotalCommentCount())
                        .content(comment.getContent())
                        .build())
                .orElseThrow(() -> new IllegalArgumentException("comment not found"));
    }

    @Override
    public List<CommentReplyResponseDto> getReplies(Long voteId, Long parentCommentId) {
        // 유효한 투표인지 확인
        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new IllegalArgumentException("투표가 존재하지 않습니다."));

        // 대댓글 조회
        List<Comment> replies = commentRepository.findAllByParentId(parentCommentId);

        return replies.stream()
                .map(reply -> {
                    MemberProfile profile = memberProfileRepository.findByMemberId(reply.getMember().getId())
                            .orElseThrow(() -> new IllegalArgumentException("회원 프로필이 존재하지 않습니다."));

                    VoteLabel label = reply.getMember().getMemberVoteOptions().stream()
                            .filter(opt -> opt.getVoteOption().getVote().getId().equals(voteId))
                            .map(opt -> opt.getVoteOption().getLabel())
                            .findFirst()
                            .orElse(null);

                    return CommentReplyResponseDto.builder()
                            .id(reply.getId())
                            .nickname(profile.getNickname())
                            .createdAt(reply.getCreatedAt())
                            .content(reply.getContent())
                            .likeCount(reply.getLikeCount())
                            .replyCount(reply.getReplyCount())
                            .isDeleted(reply.getIsDeleted())
                            .label(label)
                            .build();
                })
                .collect(Collectors.toList());
    }
}

