package com.valanse.valanse.repository;

import com.valanse.valanse.domain.VoteOption;
import com.valanse.valanse.domain.enums.VoteLabel;
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

    // vote.getVoteOptions()로 옵션을 먼저 찾으면 그 옵션이 잠금 없이 영속성
    // 컨텍스트에 올라가 버려서, 이후 findByIdForUpdate를 호출해도 Hibernate가
    // 이미 캐시된 엔티티를 그대로 돌려주고 필드를 갱신하지 않는다(#351). 이
    // 메서드로 vote.getVoteOptions()를 거치지 않고 라벨만으로 바로 잠금 조회한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select vo from VoteOption vo where vo.vote.id = :voteId and vo.label = :label")
    Optional<VoteOption> findByVoteIdAndLabelForUpdate(@Param("voteId") Long voteId, @Param("label") VoteLabel label);
}
