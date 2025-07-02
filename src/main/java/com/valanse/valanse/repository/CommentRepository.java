package com.valanse.valanse.repository;

import com.valanse.valanse.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByCommentGroupIdAndIsDeletedFalseAndParentIsNull(Long groupId);
    List<Comment> findByCommentGroupIdAndIsDeletedFalse(Long groupId);
    // 필요한 경우 사용자 정의 쿼리 메서드 추가 가능

    // 내가 작성한 댓글 목록 가져오기 (삭제되지 않은 것만)
    List<Comment> findByMemberIdAndIsDeletedFalse(Long memberId);
}
