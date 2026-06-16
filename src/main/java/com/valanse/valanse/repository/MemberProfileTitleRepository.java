package com.valanse.valanse.repository;

import com.valanse.valanse.domain.MemberProfileTitle;
import org.springframework.data.jpa.repository.JpaRepository;

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

    List<MemberProfileTitle> findAllByMemberProfileMemberId(Long memberId);

    List<MemberProfileTitle> findAllByTitleIdAndEquippedTrue(Long titleId);
}
