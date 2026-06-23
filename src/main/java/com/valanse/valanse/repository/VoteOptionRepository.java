package com.valanse.valanse.repository;

import com.valanse.valanse.domain.VoteOption;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
/**
 * VoteOptionRepository 엔티티의 DB 접근을 담당하는 레포지토리 코드입니다.
 */
public interface VoteOptionRepository extends JpaRepository<VoteOption, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select vo from VoteOption vo where vo.id = :id")
    Optional<VoteOption> findByIdForUpdate(@Param("id") Long id);
}
