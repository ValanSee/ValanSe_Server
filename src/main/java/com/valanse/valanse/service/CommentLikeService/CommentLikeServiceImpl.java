package com.valanse.valanse.service.CommentLikeService;

import com.valanse.valanse.domain.Comment;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.mapping.CommentLike;
import com.valanse.valanse.dto.Comment.CommentLikeResponseDto;
import com.valanse.valanse.repository.CommentLikeRepository;
import com.valanse.valanse.repository.CommentRepository;
import com.valanse.valanse.repository.MemberRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class CommentLikeServiceImpl implements CommentLikeService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public CommentLikeResponseDto likeComment(Long voteId, Long commentId) {
        Long userId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());

        Member member = memberRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글이 존재하지 않습니다."));

        // 이미 좋아요한 경우 → 좋아요 취소
        commentLikeRepository.findByUserIdAndCommentId(userId, commentId).ifPresentOrElse(existingLike -> {
            commentLikeRepository.delete(existingLike);
            comment.setLikeCount(comment.getLikeCount() - 1);
        }, () -> {
            // 좋아요하지 않은 경우 → 좋아요 추가
            CommentLike commentLike = CommentLike.builder()
                    .user(member)
                    .comment(comment)
                    .build();
            commentLikeRepository.save(commentLike);
            comment.setLikeCount(comment.getLikeCount() + 1);
        });

        return CommentLikeResponseDto.builder()
                .commentId(commentId)
                .likeCount(comment.getLikeCount())
                .message(commentLikeRepository.findByUserIdAndCommentId(userId, commentId).isPresent() ? "좋아요 성공" : "좋아요 취소")
                .build();
    }

}
