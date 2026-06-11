package com.valanse.valanse.repository;

import com.valanse.valanse.domain.AnonymousUserLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnonymousUserLinkRepository extends JpaRepository<AnonymousUserLink, Long> {

    Optional<AnonymousUserLink> findByAnonymousId(String anonymousId);

    boolean existsByAnonymousId(String anonymousId);
}
