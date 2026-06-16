package com.valanse.valanse.repository;

import com.valanse.valanse.domain.VoteOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
/**
 * VoteOptionRepository 엔티티의 DB 접근을 담당하는 레포지토리 코드입니다.
 */
public interface VoteOptionRepository extends JpaRepository<VoteOption, Long> {
}