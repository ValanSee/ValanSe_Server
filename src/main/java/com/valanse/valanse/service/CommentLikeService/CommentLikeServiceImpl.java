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
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional
/**
 * 댓글 좋아요 추가와 취소 및 좋아요 수 갱신을 처리하는 서비스 코드입니다.
 * check: voteId와 commentId의 소속 검증이 필요합니다.
 * check: 같은 사용자의 중복 좋아요는 DB unique 제약과 동시성 처리가 필요합니다.
 */
public class CommentLikeServiceImpl implements CommentLikeService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final MemberRepository memberRepository;

    /**
     * 댓글 좋아요를 토글하고 좋아요 수를 갱신하는 메서드입니다.
     * check: 댓글이 현재 투표에 속하는지 검증해야 합니다.
     */
    @Override
    public CommentLikeResponseDto likeComment(Long voteId, Long commentId) {
        Long userId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());

        Member member = memberRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException(MemberErrorMessage.MEMBER_NOT_FOUND.message(), HttpStatus.NOT_FOUND));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ApiException(CommentErrorMessage.COMMENT_NOT_FOUND.message(), HttpStatus.NOT_FOUND));

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
