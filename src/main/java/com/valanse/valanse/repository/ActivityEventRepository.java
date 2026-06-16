package com.valanse.valanse.repository;

import com.valanse.valanse.domain.ActivityEvent;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.enums.ActivityEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
/**
 * ActivityEventRepository 엔티티의 DB 접근을 담당하는 레포지토리 코드입니다.
 */
public interface ActivityEventRepository extends JpaRepository<ActivityEvent, Long> {

    @Modifying
    @Query("UPDATE ActivityEvent e SET e.member = :member WHERE e.anonymousId = :anonymousId AND e.member IS NULL")
    int linkAnonymousEventsToMember(@Param("anonymousId") String anonymousId, @Param("member") Member member);

    @Query("""
            SELECT COUNT(DISTINCT e.member.id)
            FROM ActivityEvent e
            WHERE e.eventType = :eventType
            AND e.createdAt >= :startAt
            AND e.createdAt < :endAt
            AND e.member IS NOT NULL
            AND e.member.deletedAt IS NULL
            """)
    long countActiveLoginUsers(
            @Param("eventType") ActivityEventType eventType,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );

    @Query("""
            SELECT COUNT(DISTINCT e.member.id)
            FROM ActivityEvent e
            WHERE e.eventType = :eventType
            AND e.createdAt >= :startAt
            AND e.createdAt < :endAt
            AND e.member IS NOT NULL
            AND e.member.deletedAt IS NOT NULL
            """)
    long countWithdrawnUsers(
            @Param("eventType") ActivityEventType eventType,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );

    @Query("""
            SELECT COUNT(DISTINCT e.anonymousId)
            FROM ActivityEvent e
            WHERE e.eventType = :eventType
            AND e.createdAt >= :startAt
            AND e.createdAt < :endAt
            AND e.member IS NULL
            AND e.anonymousId IS NOT NULL
            """)
    long countAnonymousUsers(
            @Param("eventType") ActivityEventType eventType,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );
}
