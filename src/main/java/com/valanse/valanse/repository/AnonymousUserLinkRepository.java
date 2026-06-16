package com.valanse.valanse.repository;

import com.valanse.valanse.domain.AnonymousUserLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
/**
 * AnonymousUserLinkRepository 엔티티의 DB 접근을 담당하는 레포지토리 코드입니다.
 */
public interface AnonymousUserLinkRepository extends JpaRepository<AnonymousUserLink, Long> {

    Optional<AnonymousUserLink> findByAnonymousId(String anonymousId);

    boolean existsByAnonymousId(String anonymousId);
}
