package com.valanse.valanse.repository;


import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.valanse.valanse.domain.enums.VoteCategory;


import java.util.List;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    // 내가 생성한 투표
    List<Vote> findAllByMemberOrderByCreatedAtDesc(Member member);
    List<Vote> findAllByMemberOrderByCreatedAtAsc(Member member);

    @Query("SELECT DISTINCT v FROM Vote v JOIN v.voteOptions vo JOIN vo.memberVoteOptions mvo WHERE mvo.member = :member AND v.category = :category ORDER BY v.createdAt DESC")
    List<Vote> findAllByMemberVotedAndCategoryOrderByCreatedAtDesc(@Param("member") Member member, @Param("category") VoteCategory category);

    @Query("SELECT DISTINCT v FROM Vote v JOIN v.voteOptions vo JOIN vo.memberVoteOptions mvo WHERE mvo.member = :member AND v.category = :category ORDER BY v.createdAt ASC")
    List<Vote> findAllByMemberVotedAndCategoryOrderByCreatedAtAsc(@Param("member") Member member, @Param("category") VoteCategory category);

    List<Vote> findAllByMemberAndCategoryOrderByCreatedAtDesc(Member member, VoteCategory category);

    List<Vote> findAllByMemberAndCategoryOrderByCreatedAtAsc(Member member, VoteCategory category);

    @Query("SELECT DISTINCT v FROM Vote v JOIN v.voteOptions vo JOIN vo.memberVoteOptions mvo WHERE mvo.member = :member AND v.category = :category ORDER BY v.createdAt DESC")
    List<Vote> findAllByMemberVotedAndCategoryOrderByCreatedAtDesc(@Param("member") Member member, @Param("category") String category);

    @Query("SELECT DISTINCT v FROM Vote v JOIN v.voteOptions vo JOIN vo.memberVoteOptions mvo WHERE mvo.member = :member AND v.category = :category ORDER BY v.createdAt ASC")
    List<Vote> findAllByMemberVotedAndCategoryOrderByCreatedAtAsc(@Param("member") Member member, @Param("category") String category);

    @Query("SELECT DISTINCT v FROM Vote v JOIN v.voteOptions vo JOIN vo.memberVoteOptions mvo WHERE mvo.member = :member ORDER BY v.createdAt DESC")
    List<Vote> findAllByMemberVotedOrderByCreatedAtDesc(@Param("member") Member member);

    @Query("SELECT DISTINCT v FROM Vote v JOIN v.voteOptions vo JOIN vo.memberVoteOptions mvo WHERE mvo.member = :member ORDER BY v.createdAt ASC")
    List<Vote> findAllByMemberVotedOrderByCreatedAtAsc(@Param("member") Member member);
}
