package com.valanse.valanse.repository;

import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.enums.SocialType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
/**
 * MemberRepository 엔티티의 DB 접근을 담당하는 레포지토리 코드입니다.
 */
public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findBySocialTypeAndSocialIdAndDeletedAtIsNull(SocialType socialType, String socialId);

    Optional<Member> findByIdAndDeletedAtIsNull(Long id);

    List<Member> findByIsBotTrueAndDeletedAtIsNullOrderByIdAsc();
}