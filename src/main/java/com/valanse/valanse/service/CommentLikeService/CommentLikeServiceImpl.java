package com.valanse.valanse.service.CommentLikeService;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.common.message.CommentErrorMessage;
import com.valanse.valanse.common.message.MemberErrorMessage;
import com.valanse.valanse.domain.Comment;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.mapping.CommentLike;
import com.valanse.valanse.dto.Comment.CommentLikeResponseDto;
import com.valanse.valanse.repository.CommentLikeRepository;
import com.valanse.valanse.repository.CommentRepository;
import com.valanse.valanse.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Transactional
/**
 * 댓글 좋아요 추가와 취소 및 좋아요 수 갱신을 처리하는 서비스 코드입니다.
 */
public class CommentLikeServiceImpl implements CommentLikeService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final MemberRepository memberRepository;

    /**
     * 댓글 좋아요를 토글하고 좋아요 수를 갱신하는 메서드입니다.
     */
    @Override
    public CommentLikeResponseDto likeComment(Long voteId, Long commentId) {
        Long userId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());

        Member member = memberRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException(MemberErrorMessage.MEMBER_NOT_FOUND.message(), HttpStatus.NOT_FOUND));

        Comment comment = findCommentForUpdate(commentId)
                .orElseThrow(() -> new ApiException(CommentErrorMessage.COMMENT_NOT_FOUND.message(), HttpStatus.NOT_FOUND));

        if (comment.getCommentGroup() == null
                || comment.getCommentGroup().getVote() == null
                || !voteId.equals(comment.getCommentGroup().getVote().getId())) {
            throw new ApiException(CommentErrorMessage.COMMENT_NOT_BELONG_TO_VOTE.message(), HttpStatus.BAD_REQUEST);
        }

        AtomicBoolean liked = new AtomicBoolean(false);

        // 이미 좋아요한 경우 → 좋아요 취소
        commentLikeRepository.findByUserIdAndCommentId(userId, commentId).ifPresentOrElse(existingLike -> {
            commentLikeRepository.delete(existingLike);
            comment.setLikeCount(decrementCount(comment.getLikeCount()));
        }, () -> {
            // 좋아요하지 않은 경우 → 좋아요 추가
            CommentLike commentLike = CommentLike.builder()
                    .user(member)
                    .comment(comment)
                    .build();
            saveCommentLike(commentLike);
            comment.setLikeCount(incrementCount(comment.getLikeCount()));
            liked.set(true);
        });

        return CommentLikeResponseDto.builder()
                .commentId(commentId)
                .likeCount(comment.getLikeCount())
                .message(liked.get() ? "좋아요 성공" : "좋아요 취소")
                .build();
    }

    private Optional<Comment> findCommentForUpdate(Long commentId) {
        Optional<Comment> lockedComment = commentRepository.findByIdForUpdate(commentId);
        if (lockedComment != null && lockedComment.isPresent()) {
            return lockedComment;
        }
        return commentRepository.findById(commentId);
    }

    private void saveCommentLike(CommentLike commentLike) {
        try {
            commentLikeRepository.save(commentLike);
            commentLikeRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(CommentErrorMessage.COMMENT_LIKE_DUPLICATED.message(), HttpStatus.BAD_REQUEST);
        }
    }

    private int incrementCount(Integer count) {
        return (count == null ? 0 : count) + 1;
    }

    private int decrementCount(Integer count) {
        return Math.max(0, (count == null ? 0 : count) - 1);
    }

}
