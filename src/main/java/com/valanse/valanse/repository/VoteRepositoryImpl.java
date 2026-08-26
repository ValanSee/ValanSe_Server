package com.valanse.valanse.repository;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.valanse.valanse.domain.QCommentGroup;
import com.valanse.valanse.domain.QMember;
import com.valanse.valanse.domain.QMemberProfile;
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
/**
 * VoteRepositoryImpl의 커스텀 조회 로직을 QueryDSL로 구현하는 레포지토리 코드입니다.
 */
public class VoteRepositoryImpl implements VoteRepositoryCustom {

    private final JPAQueryFactory queryFactory;
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

    /**
     * 투표 수와 댓글 수를 합산해 핫이슈 후보를 조회하는 메서드입니다.
     */
    @Override
    public Optional<Vote> findHotIssueVote() {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(vote)
                        .leftJoin(vote.commentGroup, commentGroup)
                        .orderBy(
                        vote.totalVoteCount
                                .add(commentGroup.totalCommentCount.coalesce(0))
                                .desc(),
                        vote.createdAt.desc()  // 점수 같을 때 최신순
                        )
                        .fetchFirst());
    }

    /**
     * 기간 내 댓글 또는 투표 활동이 있는 투표 중 반응성이 높은 항목을 조회하는 메서드입니다.
     */
    @Override
    public Optional<Vote> findTrendingVote(LocalDateTime from, LocalDateTime to) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(vote)
                        .leftJoin(vote.commentGroup, commentGroup)
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
