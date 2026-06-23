package com.valanse.valanse.repository;

import com.valanse.valanse.domain.MemberProfile;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * MemberProfileRepository 엔티티의 DB 접근을 담당하는 레포지토리 코드입니다.
 */
public interface MemberProfileRepository extends JpaRepository<MemberProfile, Long> {

    Optional<MemberProfile> findByMemberId(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select mp from MemberProfile mp where mp.member.id = :memberId")
    Optional<MemberProfile> findByMemberIdForUpdate(@Param("memberId") Long memberId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update MemberProfile mp set mp.point = mp.point + :amount where mp.member.id = :memberId")
    int addPointAtomically(@Param("memberId") Long memberId, @Param("amount") long amount);

    boolean existsByNicknameAndDeletedAtIsNull(String nickname);

    List<MemberProfile> findAllByDeletedAtIsNullOrderByPointDesc();

}
