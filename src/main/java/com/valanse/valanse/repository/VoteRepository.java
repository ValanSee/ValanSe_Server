// src/main/java/com/valanse/valanse/repository/VoteRepository.java
package com.valanse.valanse.repository;

import com.valanse.valanse.domain.Vote; //
import com.valanse.valanse.domain.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.time.LocalDateTime;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {

    // 가장 많은 totalVoteCount를 가진 투표 중 가장 최근에 생성된 투표를 조회
    Optional<Vote> findTopByOrderByTotalVoteCountDescCreatedAtDesc(); //

    // 추가: 특정 생성일시 이후의 투표 중 가장 많은 투표수를 가진 투표를 조회
    Optional<Vote> findTopByCreatedAtAfterOrderByTotalVoteCountDescCreatedAtDesc(LocalDateTime createdAt);
}