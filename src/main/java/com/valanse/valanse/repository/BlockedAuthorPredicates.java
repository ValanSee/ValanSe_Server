package com.valanse.valanse.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.JPAExpressions;
import com.valanse.valanse.domain.mapping.QMemberBlock;

/**
 * 차단한 회원이 작성한 콘텐츠를 목록 조회에서 제외하기 위한 QueryDSL 조건 모음입니다.
 */
public final class BlockedAuthorPredicates {

    private BlockedAuthorPredicates() {
    }

    /**
     * 작성자(authorId)가 blockerId에게 차단되지 않은 경우에만 참인 조건을 반환합니다.
     * blockerId가 null(비로그인)이면 null을 반환해 QueryDSL where 절에서 무시되게 합니다.
     * NOT EXISTS를 쓰므로 퍼지로 작성자가 null이 된 콘텐츠도 제외되지 않습니다.
     */
    public static BooleanExpression authorNotBlockedBy(Long blockerId, NumberPath<Long> authorId) {
        if (blockerId == null) {
            return null;
        }
        QMemberBlock memberBlock = new QMemberBlock("blockFilter");
        return JPAExpressions.selectOne()
                .from(memberBlock)
                .where(
                        memberBlock.blocker.id.eq(blockerId),
                        memberBlock.blocked.id.eq(authorId)
                )
                .notExists();
    }
}
