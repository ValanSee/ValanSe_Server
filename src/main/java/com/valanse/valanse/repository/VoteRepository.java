package com.valanse.valanse.repository;

import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.Vote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VoteRepository extends JpaRepository<Vote, Long> {
    List<Vote> findAllByMemberOrderByCreatedAtDesc(Member member);
    List<Vote> findAllByMemberOrderByCreatedAtAsc(Member member);
}
