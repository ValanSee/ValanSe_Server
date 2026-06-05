package com.valanse.valanse.repository.ReportRepositoryCustom;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.common.message.ReportErrorMessage;
import com.valanse.valanse.domain.Comment;
import com.valanse.valanse.domain.QReport;
import com.valanse.valanse.domain.Vote;
import com.valanse.valanse.domain.enums.ReportType;
import com.valanse.valanse.dto.Report.ReportedCommentResponse;
import com.valanse.valanse.dto.Report.ReportedTargetResponse;
import com.valanse.valanse.dto.Report.ReportedVoteResponse;
import com.valanse.valanse.repository.CommentRepository;
import com.valanse.valanse.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

@Repository
@RequiredArgsConstructor
public class ReportRepositoryImpl implements ReportRepositoryCustom {
    private final JPAQueryFactory queryFactory;
    private final VoteRepository voteRepository;
    private final CommentRepository commentRepository;

    @Override
    public List<ReportedTargetResponse> findReportedTargets(ReportType type, String sort) {
        QReport report = QReport.report;

        // 기본은 최신순, sort 값에 따라서 인기순 정렬 가능
        List<Tuple> tuples = queryFactory
                .select(report.targetId, report.count())
                .from(report)
                .where(report.reportType.eq(type))
                .groupBy(report.targetId)
                .orderBy(
                        sort.equalsIgnoreCase("popular")
                                ? report.count().desc()
                                : report.createdAt.max().desc()
                )
                .fetch();
        // type에 따라서 분기 처리
        return tuples.stream()
                .map(t -> {
                    Long targetId = t.get(report.targetId);
                    Long count = t.get(report.count());

                    if (type == ReportType.VOTE) {
                        Vote vote = voteRepository.findByIdAndDeletedAtIsNull(targetId)
                                .orElseThrow(() -> new ApiException(ReportErrorMessage.REPORTED_VOTE_NOT_FOUND.message(), HttpStatus.NOT_FOUND));
                        return ReportedTargetResponse.builder()
                                .targetId(targetId)
                                .reportCount(count)
                                .targetType("VOTE")
                                .vote(vote != null ? new ReportedVoteResponse(vote) : null)
                                .build();
                    } else if (type == ReportType.COMMENT) {
                        Comment comment = commentRepository.findByIdAndDeletedAtIsNull(targetId).
                                orElseThrow(() -> new ApiException(ReportErrorMessage.REPORTED_COMMENT_NOT_FOUND.message(), HttpStatus.NOT_FOUND));
                        return ReportedTargetResponse.builder()
                                .targetId(targetId)
                                .reportCount(count)
                                .targetType("COMMENT")
                                .comment(comment != null ? new ReportedCommentResponse(comment) : null)
                                .build();
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .toList();
    }

}
