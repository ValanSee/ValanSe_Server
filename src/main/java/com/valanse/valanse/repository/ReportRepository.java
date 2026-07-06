package com.valanse.valanse.repository;

import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.Report;
import com.valanse.valanse.domain.enums.ReportType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
/**
 * ReportRepository 엔티티의 DB 접근을 담당하는 레포지토리 코드입니다.
 */
public interface ReportRepository extends JpaRepository<Report, Long> {
    boolean existsByMemberAndReportTypeAndTargetId(Member member, ReportType reportType, Long targetId);

    @Query("select r from Report r join fetch r.member " +
            "where r.reportType = :reportType and r.targetId = :targetId " +
            "order by r.createdAt desc")
    List<Report> findDetailsByTarget(
            @Param("reportType") ReportType reportType,
            @Param("targetId") Long targetId);
}
