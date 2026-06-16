package com.valanse.valanse.repository;

import com.valanse.valanse.domain.MemberProfileTitle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * MemberProfileTitleRepository 엔티티의 DB 접근을 담당하는 레포지토리 코드입니다.
 */
public interface MemberProfileTitleRepository extends JpaRepository<MemberProfileTitle, Long> {
    Optional<MemberProfileTitle> findByMemberProfileMemberIdAndTitleId(Long memberId, Long titleId);

    Optional<MemberProfileTitle> findByMemberProfileIdAndTitleId(Long memberProfileId, Long titleId);

    List<MemberProfileTitle> findAllByMemberProfileMemberIdAndEquippedTrue(Long memberId);

    Optional<MemberProfileTitle> findByMemberProfileMemberIdAndEquippedTrue(Long memberId);

    @Query("""
            select memberProfileTitle
            from MemberProfileTitle memberProfileTitle
            join fetch memberProfileTitle.memberProfile memberProfile
            join fetch memberProfileTitle.title title
            where memberProfile.member.id in :memberIds
              and memberProfileTitle.equipped = true
            """)
    List<MemberProfileTitle> findAllEquippedWithTitleByMemberIds(@Param("memberIds") List<Long> memberIds);

    List<MemberProfileTitle> findAllByMemberProfileMemberId(Long memberId);

    List<MemberProfileTitle> findAllByTitleIdAndEquippedTrue(Long titleId);
}
