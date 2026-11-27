package com.valanse.valanse.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "daily_vote_participation",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_dvp_date_vote_member", columnNames = {"vote_date", "vote_id", "member_id"})
        },
        indexes = {
                @Index(name = "idx_dvp_date_member", columnList = "vote_date, member_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
/**
 * 회원의 오늘의 밸런스 참여 기록(첫 선택, 현재 선택, 지급 포인트)을 저장하는 JPA 도메인 엔티티 코드입니다.
 */
public class DailyVoteParticipation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vote_date", nullable = false)
    private LocalDate voteDate;

    @Column(name = "vote_id")
    private Long voteId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    private Long firstOptionId;

    private Long currentOptionId;

    @Column(nullable = false)
    private int participationPoint;

    private Integer bonusPoint;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /**
     * 현재 선택을 바꿉니다. null 이면 투표를 취소한 상태입니다(첫 선택은 유지).
     */
    public void changeCurrentOption(Long optionIdOrNull) {
        this.currentOptionId = optionIdOrNull;
    }

    /**
     * 정산 시 지급한 보너스 포인트를 기록합니다(실제로 지급한 값과 같아야 합니다).
     */
    public void recordBonus(int bonusPoint) {
        this.bonusPoint = bonusPoint;
    }
}
