// src/main/java/com/valanse/valanse/repository/VoteRepository.java
package com.valanse.valanse.repository;

import com.valanse.valanse.domain.Vote; //
import com.valanse.valanse.domain.Vote;
import com.valanse.valanse.domain.enums.VoteCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // 특정 카테고리에 해당하는 투표를 페이징하여 조회
    Page<Vote> findByCategory(VoteCategory category, Pageable pageable);

    // 모든 투표를 페이징하여 조회 (JpaRepository의 findAll(Pageable)을 사용)
    // Page<Vote> findAll(Pageable pageable); // JpaRepository에 이미 정의되어 있으므로 명시적으로 추가할 필요 없음
}