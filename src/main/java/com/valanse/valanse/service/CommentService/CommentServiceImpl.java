package com.valanse.valanse.service.CommentService;

import com.valanse.valanse.domain.*;
import com.valanse.valanse.dto.Comment.CommentPostRequest;
import com.valanse.valanse.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final MemberRepository memberRepository;
    private final VoteRepository voteRepository;
    private final CommentGroupRepository commentGroupRepository;
    private final CommentRepository commentRepository;

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
}

