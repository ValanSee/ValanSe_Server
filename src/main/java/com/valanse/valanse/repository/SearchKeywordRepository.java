package com.valanse.valanse.repository;

import com.valanse.valanse.domain.SearchKeyword;
import com.valanse.valanse.domain.enums.SearchKeywordSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SearchKeywordRepository extends JpaRepository<SearchKeyword, Long> {
    Optional<SearchKeyword> findBySourceAndNormalizedKeyword(SearchKeywordSource source, String normalizedKeyword);

    List<SearchKeyword> findByLastSeenAtGreaterThanEqual(LocalDateTime threshold);

    List<SearchKeyword> findByLastUsedAtGreaterThanEqual(LocalDateTime threshold);
}
