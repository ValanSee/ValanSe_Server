package com.valanse.valanse.repository;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.valanse.valanse.domain.QCommentGroup;
import com.valanse.valanse.domain.QVote;
import com.valanse.valanse.domain.Vote;
import com.valanse.valanse.domain.enums.VoteCategory;
import com.valanse.valanse.repository.VotesCheckRepositoryCustom.VoteRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.valanse.valanse.domain.QComment.comment;
import static com.valanse.valanse.domain.QVoteOption.voteOption;
import static com.valanse.valanse.domain.mapping.QMemberVoteOption.memberVoteOption;

@Repository
@RequiredArgsConstructor
public class VoteRepositoryImpl implements VoteRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final QVote vote = QVote.vote;
    private final QCommentGroup commentGroup = QCommentGroup.commentGroup;

    @Override
    public List<Vote> findVotesByCursor(String category, String sort, String cursor, int size) {

        List<Predicate> whereConditions = new ArrayList<>();
        if (!"ALL".equalsIgnoreCase(category)) {
            whereConditions.add(vote.category.eq(VoteCategory.valueOf(category.toUpperCase())));
        }

        // 1. orderBy를 먼저 설정하여 어떤 정렬 기준을 사용할지 결정
        OrderSpecifier<?> orderBy;
        if ("popular".equalsIgnoreCase(sort)) {
            orderBy = vote.totalVoteCount.desc();
        } else { // 기본값 또는 latest 정렬
            orderBy = vote.createdAt.desc();
        }

        // 2. cursor 값에 따라 Predicate를 생성
        Predicate cursorPredicate = null;
        if (cursor != null) {
            if ("popular".equalsIgnoreCase(sort)) {
                String[] parts = cursor.split("_");
                Integer cursorTotalVoteCount = Integer.parseInt(parts[0]);
                LocalDateTime cursorCreatedAt = LocalDateTime.parse(parts[1]);
                cursorPredicate = vote.totalVoteCount.lt(cursorTotalVoteCount)
                        .or(vote.totalVoteCount.eq(cursorTotalVoteCount).and(vote.createdAt.lt(cursorCreatedAt)));
            } else { // latest
                cursorPredicate = vote.createdAt.lt(LocalDateTime.parse(cursor));
            }
        }

        if (cursorPredicate != null) {
            whereConditions.add(cursorPredicate);
        }

        return queryFactory
                .selectFrom(vote)
                .where(whereConditions.toArray(new Predicate[0]))
                .orderBy(orderBy)
                .limit(size + 1) // 다음 페이지 존재 여부 확인
                .fetch();
    }

    @Override
    public Optional<Vote> findHotIssueVote() {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(vote)
                        .leftJoin(vote.commentGroup, commentGroup)
                        .fetchJoin()
                        .orderBy(
                        vote.totalVoteCount
                                .add(commentGroup.totalCommentCount.coalesce(0))
                                .desc(),
                        vote.createdAt.desc()  // 점수 같을 때 최신순
                        )
                        .fetchFirst());
    }

    @Override
    public Optional<Vote> findTrendingVote(LocalDateTime from, LocalDateTime to) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(vote)
                        .leftJoin(vote.commentGroup, commentGroup)
                        .fetchJoin()
                        .where(
                                // 기간 내 댓글 존재
                                JPAExpressions
                                        .selectOne()
                                        .from(comment)
                                        .where(
                                                comment.commentGroup.eq(commentGroup),
                                                comment.createdAt.between(from, to),
                                                comment.deletedAt.isNull()
                                        )
                                        .exists()
                                        .or(
                                                // 기간 내 투표 존재
                                                JPAExpressions
                                                        .selectOne()
                                                        .from(memberVoteOption)
                                                        .join(memberVoteOption.voteOption, voteOption)
                                                        .where(
                                                                voteOption.vote.eq(vote),
                                                                memberVoteOption.createdAt.between(from, to)
                                                        )
                                                        .exists()
                                        )
                        )
                        .orderBy(
                                vote.totalVoteCount
                                        .add(commentGroup.totalCommentCount.coalesce(0))
                                        .desc(),
                                vote.createdAt.desc()  // 점수 같을 때 최신순
                        )
                        .fetchFirst());

    }

}