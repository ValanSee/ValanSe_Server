package com.valanse.valanse.repository;

import com.valanse.valanse.domain.PointHistory;
import com.valanse.valanse.domain.enums.PointType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {
    List<PointHistory> findByMemberId(Long memberId);

    @Query("SELECT COUNT(p) FROM PointHistory p WHERE p.member.id = :memberId AND p.type = :type AND p.createdAt >= :from")
    long countByMemberIdAndTypeAndCreatedAtAfter(
            @Param("memberId") Long memberId,
            @Param("type") PointType type,
            @Param("from") LocalDateTime from
    );
}
