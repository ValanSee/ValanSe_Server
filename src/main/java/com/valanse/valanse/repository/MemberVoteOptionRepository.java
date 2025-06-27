package com.valanse.valanse.repository;

import com.valanse.valanse.domain.mapping.MemberVoteOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberVoteOptionRepository extends JpaRepository<MemberVoteOption, Long> {
    // 특정 멤버가 특정 투표에 대해 이전에 투표한 기록이 있는지 찾습니다.
    Optional<MemberVoteOption> findByMemberIdAndVoteId(Long memberId, Long voteId);
}