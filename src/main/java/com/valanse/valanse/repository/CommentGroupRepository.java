package com.valanse.valanse.repository;

import com.valanse.valanse.domain.CommentGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * CommentGroupRepository 엔티티의 DB 접근을 담당하는 레포지토리 코드입니다.
 */
public interface CommentGroupRepository extends JpaRepository<CommentGroup, Long> {
    Optional<CommentGroup> findByVoteId(Long voteId);
}
