package com.valanse.valanse.repository.ReportRepositoryCustom;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.valanse.valanse.domain.QReport;
import com.valanse.valanse.domain.enums.ReportType;
import com.valanse.valanse.dto.Report.ReportedTargetResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReportRepositoryImpl implements ReportRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<ReportedTargetResponse> findReportedTargets(ReportType type, String sort) {
        QReport report = QReport.report;

        List<Tuple> tuples = queryFactory
                .select(report.targetId, report.count())
                .from(report)
                .where(report.reportType.eq(type))
                .groupBy(report.targetId)
                .orderBy(
                        sort.equalsIgnoreCase("popular")
                                ? report.count().desc()
                                : report.createdAt.desc()
                )
                .fetch();
        return tuples.stream()
                .map( t-> new ReportedTargetResponse(
                        t.get(report.targetId),
                        t.get(report.count())
                )).toList();
    }

    @Override
    public long countReports(ReportType type, Long targetId) {
        QReport report = QReport.report;

        return Optional.ofNullable(queryFactory
                .select(report.count())
                .from(report)
                .where(
                        report.reportType.eq(type),
                        report.targetId.eq(targetId)
                )
                .fetchOne()
        ).orElse(0L);
    }

}