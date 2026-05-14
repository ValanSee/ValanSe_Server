package com.valanse.valanse.repository;

import com.valanse.valanse.domain.Title;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TitleRepository extends JpaRepository<Title, Long> {
    List<Title> findAllByActiveTrueOrderByDisplayOrderAscIdAsc();

    boolean existsByCode(String code);
}
