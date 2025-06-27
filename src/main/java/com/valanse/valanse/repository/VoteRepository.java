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

    // TODO: 추후 특정 기간 내에서 가장 많이 참여한 투표를 가져올 경우 사용
    // Optional<Vote> findTopByCreatedAtAfterOrderByTotalVoteCountDescCreatedAtDesc(LocalDateTime createdAt);
}