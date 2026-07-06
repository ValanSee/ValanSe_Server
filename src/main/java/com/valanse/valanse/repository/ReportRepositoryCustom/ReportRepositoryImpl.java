package com.valanse.valanse.repository.ReportRepositoryCustom;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.valanse.valanse.domain.QReport;
import com.valanse.valanse.domain.Comment;
import com.valanse.valanse.domain.Vote;
import com.valanse.valanse.domain.enums.ReportType;
import com.valanse.valanse.dto.Report.ReportedCommentResponse;
import com.valanse.valanse.dto.Report.ReportedTargetResponse;
import com.valanse.valanse.dto.Report.ReportedVoteResponse;
import com.valanse.valanse.repository.CommentRepository;
import com.valanse.valanse.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
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

        List<Long> targetIds = tuples.stream()
                .map(tuple -> tuple.get(report.targetId))
                .filter(Objects::nonNull)
                .toList();

        if (targetIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Vote> votesById = type == ReportType.VOTE
                ? voteRepository.findAllByIdInAndDeletedAtIsNull(targetIds).stream()
                        .collect(Collectors.toMap(Vote::getId, Function.identity()))
                : Map.of();
        Map<Long, Comment> commentsById = type == ReportType.COMMENT
                ? commentRepository.findAllActiveByIdInWithReportDetails(targetIds).stream()
                        .collect(Collectors.toMap(Comment::getId, Function.identity()))
                : Map.of();

        // type에 따라서 분기 처리
        return tuples.stream()
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
    }

}
