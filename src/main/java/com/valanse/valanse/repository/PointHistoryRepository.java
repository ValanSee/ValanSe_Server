package com.valanse.valanse.repository;

import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.PointHistory;
import com.valanse.valanse.domain.enums.PointType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {
    long countByMemberAndTypeAndCreatedAtBetween(Member member, PointType type, LocalDateTime createdAtAfter, LocalDateTime createdAtBefore);
}
