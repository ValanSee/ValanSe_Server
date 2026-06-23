package com.valanse.valanse.repository;

import com.valanse.valanse.domain.CommentGroup;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * CommentGroupRepository 엔티티의 DB 접근을 담당하는 레포지토리 코드입니다.
 */
public interface CommentGroupRepository extends JpaRepository<CommentGroup, Long> {
    Optional<CommentGroup> findByVoteId(Long voteId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select cg from CommentGroup cg where cg.vote.id = :voteId")
    Optional<CommentGroup> findByVoteIdForUpdate(@Param("voteId") Long voteId);
}
