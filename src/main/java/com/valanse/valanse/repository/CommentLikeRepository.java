package com.valanse.valanse.repository;

import com.valanse.valanse.domain.mapping.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * CommentLikeRepository 엔티티의 DB 접근을 담당하는 레포지토리 코드입니다.
 */
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
    Optional<CommentLike> findByUserIdAndCommentId(Long userId, Long commentId);
}
