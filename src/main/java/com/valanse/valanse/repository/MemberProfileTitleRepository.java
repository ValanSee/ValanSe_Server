package com.valanse.valanse.repository;

import com.valanse.valanse.domain.MemberProfileTitle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberProfileTitleRepository extends JpaRepository<MemberProfileTitle, Long> {
    Optional<MemberProfileTitle> findByMemberProfileMemberIdAndTitleId(Long memberId, Long titleId);

    Optional<MemberProfileTitle> findByMemberProfileIdAndTitleId(Long memberProfileId, Long titleId);

    List<MemberProfileTitle> findAllByMemberProfileMemberIdAndEquippedTrue(Long memberId);

    List<MemberProfileTitle> findAllByMemberProfileMemberId(Long memberId);

    List<MemberProfileTitle> findAllByTitleIdAndEquippedTrue(Long titleId);
}
