package com.valanse.valanse.repository.CommentRepositoryCustom;

//import com.querydsl.core.types.dsl.CaseBuilder;
//import static com.querydsl.core.types.dsl.Expressions.constant;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberTemplate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.valanse.valanse.domain.*;
import com.valanse.valanse.domain.enums.Role;
import com.valanse.valanse.domain.mapping.QMemberVoteOption;
import com.valanse.valanse.dto.Comment.CommentResponseDto;
import com.valanse.valanse.dto.Comment.QCommentResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
/**
 * CommentRepositoryImpl의 커스텀 조회 로직을 QueryDSL로 구현하는 레포지토리 코드입니다.
 */
public class CommentRepositoryImpl implements CommentRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * CommentsByVoteIdSlice 조건에 맞는 데이터를 찾는 메서드입니다.
     */
    @Override
    public Slice<CommentResponseDto> findCommentsByVoteIdSlice(Long voteId, String sort, Pageable pageable, Long loginId,boolean isAdmin) {
        QComment comment = QComment.comment;
        QMember member = QMember.member;
        QVote vote = QVote.vote;
        QMemberVoteOption mvo = QMemberVoteOption.memberVoteOption;
        QVoteOption voteOption = QVoteOption.voteOption;
        QMemberProfile profile = QMemberProfile.memberProfile;
        QMemberProfileTitle memberProfileTitle = QMemberProfileTitle.memberProfileTitle;
        QTitle title = QTitle.title;

        NumberTemplate<Long> totalHoursAgo = Expressions.numberTemplate(
                Long.class,
                "timestampdiff(hour, {0}, current_timestamp)",
                comment.createdAt
        );

        NumberTemplate<Long> daysAgo = Expressions.numberTemplate(
                Long.class,
                "floor(timestampdiff(hour, {0}, current_timestamp) / 24)",
                comment.createdAt
        );

        NumberTemplate<Long> hoursAgo = Expressions.numberTemplate(
                Long.class,
                "mod(timestampdiff(hour, {0}, current_timestamp), 24)",
                comment.createdAt
        );

        BooleanExpression canDelete = Expressions.asBoolean(false);
        if (loginId != null) {
            canDelete = isAdmin
                    ? Expressions.asBoolean(true)  // 관리자면 무조건 true
                    : comment.member.id.eq(loginId);

        }


        List<CommentResponseDto> result = queryFactory
                .select(new QCommentResponseDto(
                        comment.id,
                        vote.id,
                        profile.nickname,
                        title.name,
                        comment.createdAt,
                        vote.createdAt,
                        comment.content,
                        comment.likeCount,
                        comment.replyCount,
                        comment.deletedAt,
                        voteOption.label.stringValue(),
                        daysAgo,
                        hoursAgo,
                        canDelete
                        ))
                .from(comment)
                .join(comment.member, member)
                .leftJoin(member.profile, profile)
                .leftJoin(profile.memberProfileTitles, memberProfileTitle).on(memberProfileTitle.equipped.isTrue())
                .leftJoin(memberProfileTitle.title, title)
                .join(comment.commentGroup.vote, vote)
                .leftJoin(mvo).on(mvo.member.eq(member).and(mvo.vote.eq(vote)))
                .leftJoin(mvo.voteOption, voteOption)
                .where(vote.id.eq(voteId), comment.parent.isNull(), comment.deletedAt.isNull()) // deletedAt 조건 추가
                .orderBy(sort.equals("latest") ? comment.createdAt.desc() : comment.likeCount.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)
                .fetch();

        boolean hasNext = result.size() > pageable.getPageSize();
        if (hasNext) {
            result.remove(result.size() - 1);
        }

        return new SliceImpl<>(result, pageable, hasNext);
    }

    /**
     * MostLikedCommentByVoteId 조건에 맞는 데이터를 찾는 메서드입니다.
     */
    @Override
    public Optional<Comment> findMostLikedCommentByVoteId(Long voteId) {
        QComment comment = QComment.comment;
        QCommentGroup group = QCommentGroup.commentGroup;

        return Optional.ofNullable(
                queryFactory
                        .selectFrom(comment)
                        .join(comment.commentGroup, group)
                        .where(
                                group.vote.id.eq(voteId),
                                comment.deletedAt.isNull()
                        )
                        .orderBy(comment.likeCount.desc())
                        .fetchFirst()
        );
    }

    /**
     * CommentRepositoryImpl의 countActiveCommentsByVoteId 기능을 수행하는 메서드입니다.
     */
    @Override
    public Long countActiveCommentsByVoteId(Long voteId) {
        QComment comment = QComment.comment;
        QCommentGroup group = QCommentGroup.commentGroup;

        return queryFactory
                .select(comment.count())
                .from(comment)
                .join(comment.commentGroup, group)
                .where(
                        group.vote.id.eq(voteId),
                        comment.deletedAt.isNull(),
                        comment.parent.isNull()  // 최상위 댓글만 카운트
                )
                .fetchOne();
    }
}
