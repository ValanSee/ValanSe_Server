package com.valanse.valanse.repository;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.valanse.valanse.domain.QCommentGroup;
import com.valanse.valanse.domain.QMember;
import com.valanse.valanse.domain.QMemberProfile;
import com.valanse.valanse.domain.QVote;
import com.valanse.valanse.domain.Vote;
import com.valanse.valanse.domain.enums.VoteCategory;
import com.valanse.valanse.dto.Vote.TrendingVoteScore;
import com.valanse.valanse.repository.VotesCheckRepositoryCustom.VoteRepositoryCustom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.valanse.valanse.domain.QVoteOption.voteOption;

@Repository
@RequiredArgsConstructor
/**
 * VoteRepositoryImpl의 커스텀 조회 로직을 QueryDSL로 구현하는 레포지토리 코드입니다.
 */
public class VoteRepositoryImpl implements VoteRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;
    private final QVote vote = QVote.vote;
    private final QMember member = QMember.member;
    private final QMemberProfile memberProfile = QMemberProfile.memberProfile;
    private final QCommentGroup commentGroup = QCommentGroup.commentGroup;

    /**
     * 투표 목록 커서 페이지네이션에 필요한 ID 조회와 fetch join 조회를 수행하는 메서드입니다.
     */
    @Override
    public List<Vote> findVotesByCursor(String category, String sort, String cursor, int size) {

        List<Predicate> whereConditions = new ArrayList<>();
        if (!"ALL".equalsIgnoreCase(category)) {
            whereConditions.add(vote.category.eq(VoteCategory.valueOf(category.toUpperCase())));
        }

        // 1. orderBy를 먼저 설정하여 어떤 정렬 기준을 사용할지 결정
        OrderSpecifier<?>[] orderBy;
        if ("popular".equalsIgnoreCase(sort)) {
            orderBy = new OrderSpecifier<?>[]{
                    vote.totalVoteCount.desc(),
                    vote.createdAt.desc(),
                    vote.id.desc()
            };
        } else { // 기본값 또는 latest 정렬
            orderBy = new OrderSpecifier<?>[]{
                    vote.createdAt.desc(),
                    vote.id.desc()
            };
        }

        // 2. cursor 값에 따라 Predicate를 생성
        Predicate cursorPredicate = null;
        if (cursor != null) {
            if ("popular".equalsIgnoreCase(sort)) {
                String[] parts = cursor.split("_");
                Integer cursorTotalVoteCount = Integer.parseInt(parts[0]);
                LocalDateTime cursorCreatedAt = LocalDateTime.parse(parts[1]);
                Long cursorId = Long.parseLong(parts[2]);
                cursorPredicate = vote.totalVoteCount.lt(cursorTotalVoteCount)
                        .or(vote.totalVoteCount.eq(cursorTotalVoteCount)
                                .and(vote.createdAt.lt(cursorCreatedAt)))
                        .or(vote.totalVoteCount.eq(cursorTotalVoteCount)
                                .and(vote.createdAt.eq(cursorCreatedAt))
                                .and(vote.id.lt(cursorId)));
            } else { // latest
                String[] parts = cursor.split("_");
                LocalDateTime cursorCreatedAt = LocalDateTime.parse(parts[0]);
                Long cursorId = Long.parseLong(parts[1]);
                cursorPredicate = vote.createdAt.lt(cursorCreatedAt)
                        .or(vote.createdAt.eq(cursorCreatedAt).and(vote.id.lt(cursorId)));
            }
        }

        if (cursorPredicate != null) {
            whereConditions.add(cursorPredicate);
        }

        List<Long> voteIds = queryFactory
                .select(vote.id)
                .from(vote)
                .where(whereConditions.toArray(new Predicate[0]))
                .orderBy(orderBy)
                .limit(size + 1) // 다음 페이지 존재 여부 확인
                .fetch();

        if (voteIds.isEmpty()) {
            return new ArrayList<>();
        }

        return queryFactory
                .selectFrom(vote)
                .distinct()
                .leftJoin(vote.member, member).fetchJoin()
                .leftJoin(member.profile, memberProfile).fetchJoin()
                .leftJoin(vote.commentGroup, commentGroup).fetchJoin()
                .leftJoin(vote.voteOptions, voteOption).fetchJoin()
                .where(vote.id.in(voteIds))
                .orderBy(orderBy)
                .fetch();
    }

    @Override
    public List<TrendingVoteScore> findTopTrendingVotes(
            LocalDateTime from,
            LocalDateTime to,
            int limit
    ) {
        Query query = entityManager.createNativeQuery("""
                SELECT reactions.vote_id,
                       SUM(reactions.vote_count) AS vote_reaction_count,
                       SUM(reactions.comment_count) AS comment_reaction_count,
                       SUM(reactions.vote_count + reactions.comment_count) AS reactivity_score,
                       MAX(reactions.latest_reaction_at) AS latest_reaction_at
                FROM (
                    SELECT mvo.vote_id,
                           COUNT(*) AS vote_count,
                           0 AS comment_count,
                           MAX(mvo.created_at) AS latest_reaction_at
                    FROM member_vote_option mvo
                    LEFT JOIN member m ON m.id = mvo.member_id
                    WHERE mvo.created_at >= :from
                      AND mvo.created_at < :to
                      AND mvo.deleted_at IS NULL
                      AND (m.id IS NULL OR m.is_bot = FALSE)
                    GROUP BY mvo.vote_id

                    UNION ALL

                    SELECT cg.vote_id,
                           0 AS vote_count,
                           COUNT(*) AS comment_count,
                           MAX(c.created_at) AS latest_reaction_at
                    FROM `comment` c
                    JOIN comment_group cg ON cg.id = c.comment_group_id
                    LEFT JOIN member m ON m.id = c.member_id
                    WHERE c.created_at >= :from
                      AND c.created_at < :to
                      AND c.deleted_at IS NULL
                      AND (m.id IS NULL OR m.is_bot = FALSE)
                    GROUP BY cg.vote_id
                ) reactions
                JOIN vote v ON v.id = reactions.vote_id
                WHERE v.deleted_at IS NULL
                GROUP BY reactions.vote_id
                ORDER BY reactivity_score DESC,
                         latest_reaction_at DESC,
                         reactions.vote_id DESC
                LIMIT :limit
                """);

        query.setParameter("from", from);
        query.setParameter("to", to);
        query.setParameter("limit", limit);
        return toTrendingVoteScores(query.getResultList());
    }

    @Override
    public List<TrendingVoteScore> findTopAllTimeTrendingVotes(int limit) {
        Query query = entityManager.createNativeQuery("""
                SELECT v.id AS vote_id,
                       COALESCE(reactions.vote_count, 0) AS vote_reaction_count,
                       COALESCE(reactions.comment_count, 0) AS comment_reaction_count,
                       COALESCE(reactions.vote_count, 0) + COALESCE(reactions.comment_count, 0) AS reactivity_score,
                       reactions.latest_reaction_at
                FROM vote v
                LEFT JOIN (
                    SELECT activity.vote_id,
                           SUM(activity.vote_count) AS vote_count,
                           SUM(activity.comment_count) AS comment_count,
                           MAX(activity.latest_reaction_at) AS latest_reaction_at
                    FROM (
                        SELECT mvo.vote_id,
                               COUNT(*) AS vote_count,
                               0 AS comment_count,
                               MAX(mvo.created_at) AS latest_reaction_at
                        FROM member_vote_option mvo
                        LEFT JOIN member m ON m.id = mvo.member_id
                        WHERE mvo.deleted_at IS NULL
                          AND (m.id IS NULL OR m.is_bot = FALSE)
                        GROUP BY mvo.vote_id

                        UNION ALL

                        SELECT cg.vote_id,
                               0 AS vote_count,
                               COUNT(*) AS comment_count,
                               MAX(c.created_at) AS latest_reaction_at
                        FROM `comment` c
                        JOIN comment_group cg ON cg.id = c.comment_group_id
                        LEFT JOIN member m ON m.id = c.member_id
                        WHERE c.deleted_at IS NULL
                          AND (m.id IS NULL OR m.is_bot = FALSE)
                        GROUP BY cg.vote_id
                    ) activity
                    GROUP BY activity.vote_id
                ) reactions ON reactions.vote_id = v.id
                WHERE v.deleted_at IS NULL
                ORDER BY reactivity_score DESC,
                         latest_reaction_at DESC,
                         v.id DESC
                LIMIT :limit
                """);

        query.setParameter("limit", limit);
        return toTrendingVoteScores(query.getResultList());
    }

    @Override
    public Optional<TrendingVoteScore> findTrendingScoreByVoteId(
            Long voteId,
            LocalDateTime from,
            LocalDateTime to
    ) {
        Query query = entityManager.createNativeQuery("""
                SELECT reactions.vote_id,
                       SUM(reactions.vote_count) AS vote_reaction_count,
                       SUM(reactions.comment_count) AS comment_reaction_count,
                       SUM(reactions.vote_count + reactions.comment_count) AS reactivity_score,
                       MAX(reactions.latest_reaction_at) AS latest_reaction_at
                FROM (
                    SELECT mvo.vote_id,
                           COUNT(*) AS vote_count,
                           0 AS comment_count,
                           MAX(mvo.created_at) AS latest_reaction_at
                    FROM member_vote_option mvo
                    LEFT JOIN member m ON m.id = mvo.member_id
                    WHERE mvo.vote_id = ?1
                      AND mvo.created_at >= ?2
                      AND mvo.created_at < ?3
                      AND mvo.deleted_at IS NULL
                      AND (m.id IS NULL OR m.is_bot = FALSE)
                    GROUP BY mvo.vote_id

                    UNION ALL

                    SELECT cg.vote_id,
                           0 AS vote_count,
                           COUNT(*) AS comment_count,
                           MAX(c.created_at) AS latest_reaction_at
                    FROM `comment` c
                    JOIN comment_group cg ON cg.id = c.comment_group_id
                    LEFT JOIN member m ON m.id = c.member_id
                    WHERE cg.vote_id = ?1
                      AND c.created_at >= ?2
                      AND c.created_at < ?3
                      AND c.deleted_at IS NULL
                      AND (m.id IS NULL OR m.is_bot = FALSE)
                    GROUP BY cg.vote_id
                ) reactions
                JOIN vote v ON v.id = reactions.vote_id
                WHERE v.deleted_at IS NULL
                GROUP BY reactions.vote_id
                """);

        query.setParameter(1, voteId);
        query.setParameter(2, from);
        query.setParameter(3, to);
        return toTrendingVoteScores(query.getResultList()).stream().findFirst();
    }

    @Override
    public Optional<TrendingVoteScore> findAllTimeTrendingScoreByVoteId(Long voteId) {
        Query query = entityManager.createNativeQuery("""
                SELECT v.id AS vote_id,
                       COALESCE(reactions.vote_count, 0) AS vote_reaction_count,
                       COALESCE(reactions.comment_count, 0) AS comment_reaction_count,
                       COALESCE(reactions.vote_count, 0) + COALESCE(reactions.comment_count, 0) AS reactivity_score,
                       reactions.latest_reaction_at
                FROM vote v
                LEFT JOIN (
                    SELECT activity.vote_id,
                           SUM(activity.vote_count) AS vote_count,
                           SUM(activity.comment_count) AS comment_count,
                           MAX(activity.latest_reaction_at) AS latest_reaction_at
                    FROM (
                        SELECT mvo.vote_id,
                               COUNT(*) AS vote_count,
                               0 AS comment_count,
                               MAX(mvo.created_at) AS latest_reaction_at
                        FROM member_vote_option mvo
                        LEFT JOIN member m ON m.id = mvo.member_id
                        WHERE mvo.vote_id = ?1
                          AND mvo.deleted_at IS NULL
                          AND (m.id IS NULL OR m.is_bot = FALSE)
                        GROUP BY mvo.vote_id

                        UNION ALL

                        SELECT cg.vote_id,
                               0 AS vote_count,
                               COUNT(*) AS comment_count,
                               MAX(c.created_at) AS latest_reaction_at
                        FROM `comment` c
                        JOIN comment_group cg ON cg.id = c.comment_group_id
                        LEFT JOIN member m ON m.id = c.member_id
                        WHERE cg.vote_id = ?1
                          AND c.deleted_at IS NULL
                          AND (m.id IS NULL OR m.is_bot = FALSE)
                        GROUP BY cg.vote_id
                    ) activity
                    GROUP BY activity.vote_id
                ) reactions ON reactions.vote_id = v.id
                WHERE v.id = ?1
                  AND v.deleted_at IS NULL
                """);

        query.setParameter(1, voteId);
        return toTrendingVoteScores(query.getResultList()).stream().findFirst();
    }

    @Override
    public List<Vote> findTrendingVoteDetailsByIds(List<Long> voteIds) {
        if (voteIds.isEmpty()) {
            return List.of();
        }

        return queryFactory
                .selectFrom(vote)
                .distinct()
                .leftJoin(vote.member, member).fetchJoin()
                .leftJoin(member.profile, memberProfile).fetchJoin()
                .leftJoin(vote.voteOptions, voteOption).fetchJoin()
                .where(vote.id.in(voteIds))
                .fetch();
    }

    private List<TrendingVoteScore> toTrendingVoteScores(List<?> rows) {
        return rows.stream()
                .map(row -> toTrendingVoteScore((Object[]) row))
                .toList();
    }

    private TrendingVoteScore toTrendingVoteScore(Object[] row) {
        return new TrendingVoteScore(
                ((Number) row[0]).longValue(),
                ((Number) row[1]).longValue(),
                ((Number) row[2]).longValue(),
                ((Number) row[3]).longValue(),
                toLocalDateTime(row[4])
        );
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        throw new IllegalArgumentException("지원하지 않는 날짜 타입입니다: " + value.getClass().getName());
    }

}
