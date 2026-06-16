package com.valanse.valanse.repository;

import com.valanse.valanse.domain.MemberProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * MemberProfileRepository 엔티티의 DB 접근을 담당하는 레포지토리 코드입니다.
 */
public interface MemberProfileRepository extends JpaRepository<MemberProfile, Long> {

    Optional<MemberProfile> findByMemberId(Long id);

    boolean existsByNicknameAndDeletedAtIsNull(String nickname);

    List<MemberProfile> findAllByDeletedAtIsNullOrderByPointDesc();

}
