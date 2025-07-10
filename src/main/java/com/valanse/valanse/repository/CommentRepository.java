package com.valanse.valanse.repository;

import com.valanse.valanse.domain.Comment;

import com.valanse.valanse.repository.CommentRepositoryCustom.CommentRepositoryCustom;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long>, CommentRepositoryCustom {

    // 댓글 그룹별 조회 (부모 댓글만)
    List<Comment> findByCommentGroupIdAndDeletedAtIsNullAndParentIsNull(Long groupId);

    // 댓글 그룹 전체 조회 (부모+자식)
    List<Comment> findByCommentGroupIdAndDeletedAtIsNull(Long groupId);

    // 내가 쓴 댓글 조회
    List<Comment> findByMemberIdAndDeletedAtIsNull(Long memberId);
    List<Comment> findByMemberIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long memberId);
    List<Comment> findByMemberIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long memberId);

    @EntityGraph(attributePaths = {"member", "member.memberVoteOptions", "member.memberVoteOptions.voteOption"})
    List<Comment> findAllByParentId(Long parentId);
}