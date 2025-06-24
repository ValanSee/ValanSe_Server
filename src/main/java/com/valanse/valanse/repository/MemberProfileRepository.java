package com.valanse.valanse.repository;

import com.valanse.valanse.domain.MemberProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberProfileRepository extends JpaRepository<MemberProfile, Long> {

    Optional<MemberProfile> findByMemberId(Long id);
    boolean existsByNickname(String nickname);


}
