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
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CommentRepositoryImpl implements CommentRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<CommentResponseDto> findCommentsByVoteIdSlice(Long voteId, String sort, Pageable pageable) {
        QComment comment = QComment.comment;
        QMember member = QMember.member;
        QVote vote = QVote.vote;
        QVoteOption voteOption = QVoteOption.voteOption;

        List<CommentResponseDto> result = queryFactory
                .select(new QCommentResponseDto(
                        vote.id,
                        member.profile.nickname,
                        comment.createdAt,
                        comment.content,
                        comment.likeCount,
                        comment.replyCount,
                        comment.isDeleted,
                        voteOption.label.stringValue()
                ))
                .from(comment)
                .join(comment.member, member)
                .join(comment.commentGroup.vote, vote)
                .leftJoin(vote.voteOptions, voteOption)
                .where(vote.id.eq(voteId), comment.parent.isNull())
                .orderBy(sort.equals("latest") ? comment.createdAt.desc() : comment.likeCount.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1) // 무한 스크롤용 limit + 1
                .fetch();

        boolean hasNext = result.size() > pageable.getPageSize();
        if (hasNext) {
            result.remove(result.size() - 1);
        }

        return new SliceImpl<>(result, pageable, hasNext);
    }
}
