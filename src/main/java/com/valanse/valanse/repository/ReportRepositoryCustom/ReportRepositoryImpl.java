package com.valanse.valanse.repository.ReportRepositoryCustom;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.valanse.valanse.domain.Comment;
import com.valanse.valanse.domain.QReport;
import com.valanse.valanse.domain.Vote;
import com.valanse.valanse.domain.enums.ReportType;
import com.valanse.valanse.dto.Comment.CommentResponseDto;
import com.valanse.valanse.dto.Report.ReportedCommentResponse;
import com.valanse.valanse.dto.Report.ReportedTargetResponse;
import com.valanse.valanse.dto.Report.ReportedVoteResponse;
import com.valanse.valanse.dto.Vote.VoteResponseDto;
import com.valanse.valanse.repository.CommentRepository;
import com.valanse.valanse.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReportRepositoryImpl implements ReportRepositoryCustom {
    private final JPAQueryFactory queryFactory;
    private final VoteRepository voteRepository;
    private final CommentRepository commentRepository;

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
                                : report.createdAt.max().desc()
                )
                .fetch();
        return tuples.stream()
                .map(t -> {
                    Long targetId = t.get(report.targetId);
                    Long count = t.get(report.count());

                    if (type == ReportType.VOTE) {
                        Vote vote = voteRepository.findByIdAndDeletedAtIsNull(targetId)
                                .orElseThrow(() -> new RuntimeException("해당 밸런스게임을 찾을 수 없습니다."));
                        return ReportedTargetResponse.builder()
                                .targetId(targetId)
                                .reportCount(count)
                                .targetType("VOTE")
                                .vote(vote != null ? new ReportedVoteResponse(vote) : null)
                                .build();
                    } else if (type == ReportType.COMMENT) {
                        Comment comment = commentRepository.findByIdAndDeletedAtIsNull(targetId).
                                orElseThrow(() -> new RuntimeException("해당 댓글을 찾을 수 없습니다."));
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