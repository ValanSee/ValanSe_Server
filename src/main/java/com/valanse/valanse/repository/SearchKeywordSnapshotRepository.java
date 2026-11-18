package com.valanse.valanse.repository;

import com.valanse.valanse.domain.SearchKeyword;
import com.valanse.valanse.domain.SearchKeywordSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface SearchKeywordSnapshotRepository extends JpaRepository<SearchKeywordSnapshot, Long> {
    Optional<SearchKeywordSnapshot> findBySearchKeywordAndCollectedDate(SearchKeyword searchKeyword,
                                                                        LocalDate collectedDate);
}
