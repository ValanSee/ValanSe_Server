package com.valanse.valanse.repository.CommentRepositoryCustom;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.valanse.valanse.domain.QComment;
import com.valanse.valanse.domain.QMember;
import com.valanse.valanse.domain.QVote;
import com.valanse.valanse.domain.QVoteOption;
import com.valanse.valanse.dto.Comment.CommentResponseDto;
import com.valanse.valanse.dto.Comment.QCommentResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CommentRepositoryImpl implements CommentRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<CommentResponseDto> findCommentsByVoteIdOrderBy(Long voteId, String sort, Pageable pageable) {
        QComment comment = QComment.comment;
        QMember member = QMember.member;
        QVote vote = QVote.vote;
        QVoteOption voteOption = QVoteOption.voteOption;

        return queryFactory
                .select(new QCommentResponseDto(
                        vote.id,
                        member.profile.nickname,
                        comment.createdAt,
                        comment.content,
                        comment.likeCount,
                        comment.replyCount,
                        comment.isDeleted,
                        voteOption.label.stringValue() // enum to String
                ))
                .from(comment)
                .join(comment.member, member)
                .join(comment.commentGroup.vote, vote)
                .leftJoin(vote.voteOptions, voteOption)
                .where(vote.id.eq(voteId), comment.parent.isNull()) // 부모 댓글만
                .orderBy(
                        sort.equals("latest") ? comment.createdAt.desc() : comment.likeCount.desc()
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }
}
