package com.valanse.valanse.repository;

import com.valanse.valanse.domain.Comment;
import com.valanse.valanse.repository.CommentRepositoryCustom.CommentRepositoryCustom;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long>, CommentRepositoryCustom {
    @EntityGraph(attributePaths = {"member", "member.memberVoteOptions", "member.memberVoteOptions.voteOption"})
    List<Comment> findAllByParentId(Long parentId);
}