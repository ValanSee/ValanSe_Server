package com.valanse.valanse.repository;

import com.valanse.valanse.domain.Title;
import com.valanse.valanse.domain.enums.TitleAcquisitionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TitleRepository extends JpaRepository<Title, Long> {
    List<Title> findAllByActiveTrueOrderByDisplayOrderAscIdAsc();

    Optional<Title> findFirstByActiveTrueAndAcquisitionTypeOrderByDisplayOrderAscIdAsc(
            TitleAcquisitionType acquisitionType
    );

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);
}
