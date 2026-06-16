package com.valanse.valanse.repository;

import com.valanse.valanse.domain.Title;
import com.valanse.valanse.domain.enums.TitleAcquisitionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
/**
 * TitleRepository 엔티티의 DB 접근을 담당하는 레포지토리 코드입니다.
 */
public interface TitleRepository extends JpaRepository<Title, Long> {
    List<Title> findAllByActiveTrueOrderByDisplayOrderAscIdAsc();

    List<Title> findAllByOrderByDisplayOrderAscIdAsc();

    Optional<Title> findFirstByActiveTrueAndAcquisitionTypeOrderByDisplayOrderAscIdAsc(
            TitleAcquisitionType acquisitionType
    );

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);
}
