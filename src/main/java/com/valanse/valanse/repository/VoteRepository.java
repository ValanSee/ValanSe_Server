// src/main/java/com/valanse/valanse/repository/VoteRepository.java
package com.valanse.valanse.repository;

import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.Vote; //
import com.valanse.valanse.domain.Vote;
import com.valanse.valanse.domain.enums.PinType;
import com.valanse.valanse.domain.enums.VoteCategory;
import com.valanse.valanse.repository.VotesCheckRepositoryCustom.VoteRepositoryCustom;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
/**
 * VoteRepository 엔티티의 DB 접근을 담당하는 레포지토리 코드입니다.
 */
public interface VoteRepository extends JpaRepository<Vote, Long>, VoteRepositoryCustom {

    // 내가 생성한 투표
    List<Vote> findAllByMemberOrderByCreatedAtDesc(Member member);
    List<Vote> findAllByMemberOrderByCreatedAtAsc(Member member);
    Slice<Vote> findSliceByMemberOrderByCreatedAtDesc(Member member, Pageable pageable);
    Slice<Vote> findSliceByMemberOrderByCreatedAtAsc(Member member, Pageable pageable);

    @Query("SELECT DISTINCT v FROM Vote v JOIN v.voteOptions vo JOIN vo.memberVoteOptions mvo WHERE mvo.member = :member AND v.category = :category ORDER BY v.createdAt DESC")
    List<Vote> findAllByMemberVotedAndCategoryOrderByCreatedAtDesc(@Param("member") Member member, @Param("category") VoteCategory category);

    @Query("SELECT DISTINCT v FROM Vote v JOIN v.voteOptions vo JOIN vo.memberVoteOptions mvo WHERE mvo.member = :member AND v.category = :category AND NOT EXISTS (SELECT 1 FROM MemberBlock mb WHERE mb.blocker = :member AND mb.blocked = v.member) ORDER BY v.createdAt DESC")
    Slice<Vote> findSliceByMemberVotedAndCategoryOrderByCreatedAtDesc(@Param("member") Member member, @Param("category") VoteCategory category, Pageable pageable);

    @Query("SELECT DISTINCT v FROM Vote v JOIN v.voteOptions vo JOIN vo.memberVoteOptions mvo WHERE mvo.member = :member AND v.category = :category ORDER BY v.createdAt ASC")
    List<Vote> findAllByMemberVotedAndCategoryOrderByCreatedAtAsc(@Param("member") Member member, @Param("category") VoteCategory category);

    @Query("SELECT DISTINCT v FROM Vote v JOIN v.voteOptions vo JOIN vo.memberVoteOptions mvo WHERE mvo.member = :member AND v.category = :category AND NOT EXISTS (SELECT 1 FROM MemberBlock mb WHERE mb.blocker = :member AND mb.blocked = v.member) ORDER BY v.createdAt ASC")
    Slice<Vote> findSliceByMemberVotedAndCategoryOrderByCreatedAtAsc(@Param("member") Member member, @Param("category") VoteCategory category, Pageable pageable);

    List<Vote> findAllByMemberAndCategoryOrderByCreatedAtDesc(Member member, VoteCategory category);
    Slice<Vote> findSliceByMemberAndCategoryOrderByCreatedAtDesc(Member member, VoteCategory category, Pageable pageable);

    List<Vote> findAllByMemberAndCategoryOrderByCreatedAtAsc(Member member, VoteCategory category);
    Slice<Vote> findSliceByMemberAndCategoryOrderByCreatedAtAsc(Member member, VoteCategory category, Pageable pageable);

    @Query("SELECT DISTINCT v FROM Vote v JOIN v.voteOptions vo JOIN vo.memberVoteOptions mvo WHERE mvo.member = :member AND v.category = :category ORDER BY v.createdAt DESC")
    List<Vote> findAllByMemberVotedAndCategoryOrderByCreatedAtDesc(@Param("member") Member member, @Param("category") String category);

    @Query("SELECT DISTINCT v FROM Vote v JOIN v.voteOptions vo JOIN vo.memberVoteOptions mvo WHERE mvo.member = :member AND v.category = :category ORDER BY v.createdAt ASC")
    List<Vote> findAllByMemberVotedAndCategoryOrderByCreatedAtAsc(@Param("member") Member member, @Param("category") String category);

    @Query("SELECT DISTINCT v FROM Vote v JOIN v.voteOptions vo JOIN vo.memberVoteOptions mvo WHERE mvo.member = :member ORDER BY v.createdAt DESC")
    List<Vote> findAllByMemberVotedOrderByCreatedAtDesc(@Param("member") Member member);

    @Query("SELECT DISTINCT v FROM Vote v JOIN v.voteOptions vo JOIN vo.memberVoteOptions mvo WHERE mvo.member = :member AND NOT EXISTS (SELECT 1 FROM MemberBlock mb WHERE mb.blocker = :member AND mb.blocked = v.member) ORDER BY v.createdAt DESC")
    Slice<Vote> findSliceByMemberVotedOrderByCreatedAtDesc(@Param("member") Member member, Pageable pageable);

    @Query("SELECT DISTINCT v FROM Vote v JOIN v.voteOptions vo JOIN vo.memberVoteOptions mvo WHERE mvo.member = :member ORDER BY v.createdAt ASC")
    List<Vote> findAllByMemberVotedOrderByCreatedAtAsc(@Param("member") Member member);

    @Query("SELECT DISTINCT v FROM Vote v JOIN v.voteOptions vo JOIN vo.memberVoteOptions mvo WHERE mvo.member = :member AND NOT EXISTS (SELECT 1 FROM MemberBlock mb WHERE mb.blocker = :member AND mb.blocked = v.member) ORDER BY v.createdAt ASC")
    Slice<Vote> findSliceByMemberVotedOrderByCreatedAtAsc(@Param("member") Member member, Pageable pageable);

    // 특정 카테고리에 해당하는 투표를 페이징하여 조회
    Page<Vote> findByCategory(VoteCategory category, Pageable pageable);

    // 고정된 투표 찾기
    Optional<Vote> findByPinType(PinType pinType);
    Optional<Vote> findByIdAndDeletedAtIsNull(Long id);
    List<Vote> findAllByIdInAndDeletedAtIsNull(List<Long> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Vote v where v.id = :id")
    Optional<Vote> findByIdForUpdate(@Param("id") Long id);

    // 모든 투표를 페이징하여 조회 (JpaRepository의 findAll(Pageable)을 사용)
    // Page<Vote> findAll(Pageable pageable); // JpaRepository에 이미 정의되어 있으므로 명시적으로 추가할 필요 없음

    // 삭제되지 않은 모든 게임 제목 (검색 키워드 겹침 판정용)
    @Query("select v.title from Vote v where v.deletedAt is null")
    List<String> findAllActiveTitles();

    // 오늘의 밸런스 자동 선정 후보: 최근 생성(createdAfterUtc 이후) 최신순
    @Query("select v from Vote v where v.deletedAt is null and v.createdAt >= :createdAfterUtc " +
            "and size(v.voteOptions) = 2 " +
            "and not exists (select 1 from DailyVote dv where dv.voteId = v.id) " +
            "and not exists (select 1 from Report r where r.reportType = com.valanse.valanse.domain.enums.ReportType.VOTE and r.targetId = v.id) " +
            "order by v.createdAt desc, v.id desc")
    List<Vote> findDailyCandidatesRecent(@Param("createdAfterUtc") LocalDateTime createdAfterUtc, Pageable pageable);

    // 오늘의 밸런스 자동 선정 후보: 기간 제한 없이 반응성 점수 내림차순
    @Query("select v from Vote v where v.deletedAt is null " +
            "and size(v.voteOptions) = 2 " +
            "and not exists (select 1 from DailyVote dv where dv.voteId = v.id) " +
            "and not exists (select 1 from Report r where r.reportType = com.valanse.valanse.domain.enums.ReportType.VOTE and r.targetId = v.id) " +
            "order by v.reactivityScore desc, v.id desc")
    List<Vote> findDailyCandidatesByReactivity(Pageable pageable);
}
