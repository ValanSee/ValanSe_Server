package com.valanse.valanse.repository;

import com.valanse.valanse.domain.mapping.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * CommentLikeRepository 엔티티의 DB 접근을 담당하는 레포지토리 코드입니다.
 */
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
    Optional<CommentLike> findByUserIdAndCommentId(Long userId, Long commentId);

    /**
     * 주어진 댓글 id 목록 중 사용자가 좋아요를 누른 댓글 id만 한 번의 쿼리로 조회하는 메서드입니다.
     */
    @Query("select cl.comment.id from CommentLike cl where cl.user.id = :userId and cl.comment.id in :commentIds")
    List<Long> findCommentIdsByUserIdAndCommentIdIn(@Param("userId") Long userId,
                                                    @Param("commentIds") Collection<Long> commentIds);
}
