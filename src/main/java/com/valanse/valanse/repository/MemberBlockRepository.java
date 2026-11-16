package com.valanse.valanse.repository;

import com.valanse.valanse.domain.mapping.MemberBlock;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * MemberBlock 엔티티의 DB 접근을 담당하는 레포지토리 코드입니다.
 */
public interface MemberBlockRepository extends JpaRepository<MemberBlock, Long> {

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from MemberBlock mb where mb.blocker.id = :blockerId and mb.blocked.id = :blockedId")
    int deleteByBlockerIdAndBlockedId(@Param("blockerId") Long blockerId, @Param("blockedId") Long blockedId);

    /**
     * 차단 목록 화면용: 차단한 회원을 함께 최신순으로 조회합니다.
     */
    @EntityGraph(attributePaths = {"blocked"})
    @Query("select mb from MemberBlock mb where mb.blocker.id = :blockerId order by mb.createdAt desc, mb.id desc")
    List<MemberBlock> findAllByBlockerIdWithBlocked(@Param("blockerId") Long blockerId);
}
