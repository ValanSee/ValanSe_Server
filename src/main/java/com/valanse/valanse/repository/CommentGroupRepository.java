package com.valanse.valanse.repository;

import com.valanse.valanse.domain.CommentGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommentGroupRepository extends JpaRepository<CommentGroup, Long> {
    Optional<CommentGroup> findByVoteId(Long voteId);
}
