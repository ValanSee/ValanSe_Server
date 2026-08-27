package com.valanse.valanse.repository.ReportRepositoryCustom;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.valanse.valanse.domain.QComment;
import com.valanse.valanse.domain.QReport;
import com.valanse.valanse.domain.QVote;
import com.valanse.valanse.domain.Comment;
import com.valanse.valanse.domain.Vote;
import com.valanse.valanse.domain.enums.ReportType;
import com.valanse.valanse.dto.Report.ReportedCommentResponse;
import com.valanse.valanse.dto.Report.ReportedTargetResponse;
import com.valanse.valanse.dto.Report.ReportedVoteResponse;
import com.valanse.valanse.repository.CommentRepository;
import com.valanse.valanse.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
/**
 * ReportRepositoryImpl의 커스텀 조회 로직을 QueryDSL로 구현하는 레포지토리 코드입니다.
 */
public class ReportRepositoryImpl implements ReportRepositoryCustom {
    private final JPAQueryFactory queryFactory;
    private final VoteRepository voteRepository;
    private final CommentRepository commentRepository;

    /**
     * ReportedTargets 조건에 맞는 데이터를 찾는 메서드입니다.
     */
    @Override
    public Slice<ReportedTargetResponse> findReportedTargets(ReportType type, String sort, Pageable pageable) {
        QReport report = QReport.report;
        QVote reportedVote = QVote.vote;
        QComment reportedComment = QComment.comment;
        OrderSpecifier<?> orderSpecifier = sort.equalsIgnoreCase("popular")
                ? report.count().desc()
                : report.createdAt.max().desc();

        List<Tuple> tuples;
        if (type == ReportType.VOTE) {
            tuples = queryFactory
                    .select(report.targetId, report.count())
                    .from(report)
                    .join(reportedVote).on(reportedVote.id.eq(report.targetId))
                    .where(
                            report.reportType.eq(type),
                            reportedVote.deletedAt.isNull()
                    )
                    .groupBy(report.targetId)
                    .orderBy(orderSpecifier)
                    .offset(pageable.getOffset())
                    .limit(pageable.getPageSize() + 1)
                    .fetch();
        } else if (type == ReportType.COMMENT) {
            tuples = queryFactory
                    .select(report.targetId, report.count())
                    .from(report)
                    .join(reportedComment).on(reportedComment.id.eq(report.targetId))
                    .where(
                            report.reportType.eq(type),
                            reportedComment.deletedAt.isNull()
                    )
                    .groupBy(report.targetId)
                    .orderBy(orderSpecifier)
                    .offset(pageable.getOffset())
                    .limit(pageable.getPageSize() + 1)
                    .fetch();
        } else {
            return new SliceImpl<>(List.of(), pageable, false);
        }

        boolean hasNext = tuples.size() > pageable.getPageSize();
        if (hasNext) {
            tuples.remove(tuples.size() - 1);
        }

        List<Long> targetIds = tuples.stream()
                .map(tuple -> tuple.get(report.targetId))
                .filter(Objects::nonNull)
                .toList();

        if (targetIds.isEmpty()) {
            return new SliceImpl<>(List.of(), pageable, hasNext);
        }

        Map<Long, Vote> votesById = type == ReportType.VOTE
                ? voteRepository.findAllByIdInAndDeletedAtIsNull(targetIds).stream()
                        .collect(Collectors.toMap(Vote::getId, Function.identity()))
                : Map.of();
        Map<Long, Comment> commentsById = type == ReportType.COMMENT
                ? commentRepository.findAllActiveByIdInWithReportDetails(targetIds).stream()
                        .collect(Collectors.toMap(Comment::getId, Function.identity()))
                : Map.of();

        List<ReportedTargetResponse> responses = tuples.stream()
                .map(t -> {
                    Long targetId = t.get(report.targetId);
                    Long count = t.get(report.count());

                    if (type == ReportType.VOTE) {
                        Vote vote = votesById.get(targetId);
                        return vote == null ? null : ReportedTargetResponse.builder()
                                .targetId(targetId)
                                .reportCount(count)
                                .targetType("VOTE")
                                .vote(new ReportedVoteResponse(vote))
                                .build();
                    } else if (type == ReportType.COMMENT) {
                        Comment comment = commentsById.get(targetId);
                        return comment == null ? null : ReportedTargetResponse.builder()
                                .targetId(targetId)
                                .reportCount(count)
                                .targetType("COMMENT")
                                .comment(new ReportedCommentResponse(comment))
                                .build();
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .toList();

        return new SliceImpl<>(responses, pageable, hasNext);
    }

}
