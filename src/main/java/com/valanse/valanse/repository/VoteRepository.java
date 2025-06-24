package com.valanse.valanse.repository;

import com.valanse.valanse.domain.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {
    // 가장 많은 투표 수를 가진 투표를 조회
    Optional<Vote> findTopByOrderByTotalVoteCountDesc();
}
