package com.valanse.valanse.repository;

import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.Report;
import com.valanse.valanse.domain.enums.ReportType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    boolean existsByMemberAndReportTypeAndTargetId(Member member, ReportType reportType, Long targetId);
}
