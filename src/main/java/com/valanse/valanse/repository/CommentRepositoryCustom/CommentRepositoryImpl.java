package com.valanse.valanse.repository.CommentRepositoryCustom;

//import com.querydsl.core.types.dsl.CaseBuilder;
//import static com.querydsl.core.types.dsl.Expressions.constant;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.valanse.valanse.domain.*;
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
public class CommentRepositoryImpl implements CommentRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<CommentResponseDto> findCommentsByVoteIdSlice(Long voteId, String sort, Pageable pageable) {
        QComment comment = QComment.comment;
        QMember member = QMember.member;
        QVote vote = QVote.vote;
        QMemberVoteOption mvo = QMemberVoteOption.memberVoteOption;
        QVoteOption voteOption = QVoteOption.voteOption;

        List<CommentResponseDto> result = queryFactory
                .select(new QCommentResponseDto(
                        vote.id,
                        member.profile.nickname,
                        comment.createdAt,
                        comment.content,
                        comment.likeCount,
                        comment.replyCount,
                        comment.deletedAt,
                        voteOption.label.stringValue()
//                        new CaseBuilder()
//                                .when(voteOption.isNotNull())
//                                .then(voteOption.label.stringValue())
//                                .otherwise(constant("NONE"))
                ))
                .from(comment)
                .join(comment.member, member)
                .join(comment.commentGroup.vote, vote)
                .leftJoin(mvo).on(mvo.member.eq(member).and(mvo.vote.eq(vote)))
                .leftJoin(mvo.voteOption, voteOption)
                .where(vote.id.eq(voteId), comment.parent.isNull())
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

    @Override
    public Optional<Comment> findMostLikedCommentByVoteId(Long voteId) {
        QComment comment = QComment.comment;
        QCommentGroup group = QCommentGroup.commentGroup;

        return Optional.ofNullable(
                queryFactory
                        .selectFrom(comment)
                        .join(comment.commentGroup, group)
                        .where(group.vote.id.eq(voteId))
                        .orderBy(comment.likeCount.desc())
                        .fetchFirst()
        );
    }
}
