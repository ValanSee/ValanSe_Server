// src/main/java/com/valanse/valanse/repository/VoteRepository.java
package com.valanse.valanse.repository;

import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.Vote; //
import com.valanse.valanse.domain.Vote;
import com.valanse.valanse.domain.enums.PinType;
import com.valanse.valanse.domain.enums.VoteCategory;
import com.valanse.valanse.repository.VotesCheckRepositoryCustom.VoteRepositoryCustom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long>, VoteRepositoryCustom {

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

    //여기서 부터 영서 부분
    // 가장 많은 totalVoteCount를 가진 투표 중 가장 최근에 생성된 투표를 조회
    Optional<Vote> findTopByOrderByTotalVoteCountDescCreatedAtDesc(); //

    // 추가: 특정 생성일시 이후의 투표 중 가장 많은 투표수를 가진 투표를 조회
    Optional<Vote> findTopByCreatedAtAfterOrderByTotalVoteCountDescCreatedAtDesc(LocalDateTime createdAt);

    // 추가: 작일 동안 반응성이 가장 높은 투표 조회
    Optional<Vote> findTopByReactivityUpdatedAtBetweenOrderByReactivityScoreDescCreatedAtDesc(
            LocalDateTime start, LocalDateTime end
    );

    // 추가: 전체 기간 중 반응성이 가장 높은 투표 조회 (작일 데이터 없을 때 사용)
    Optional<Vote> findTopByOrderByReactivityScoreDescCreatedAtDesc();

    // 특정 카테고리에 해당하는 투표를 페이징하여 조회
    Page<Vote> findByCategory(VoteCategory category, Pageable pageable);

    // 고정된 투표 찾기
    Optional<Vote> findByPinType(PinType pinType);
    Optional<Vote> findByIdAndDeletedAtIsNull(Long id);

    // 모든 투표를 페이징하여 조회 (JpaRepository의 findAll(Pageable)을 사용)
    // Page<Vote> findAll(Pageable pageable); // JpaRepository에 이미 정의되어 있으므로 명시적으로 추가할 필요 없음
}
