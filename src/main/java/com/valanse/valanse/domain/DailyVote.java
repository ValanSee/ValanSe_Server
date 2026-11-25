package com.valanse.valanse.domain;

import com.valanse.valanse.domain.enums.DailyVoteSource;
import com.valanse.valanse.domain.enums.DailyVoteWinner;
import com.valanse.valanse.domain.enums.VoteLabel;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_vote")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
/**
 * 날짜별 오늘의 밸런스 게임과 정산 결과를 저장하는 JPA 도메인 엔티티 코드입니다.
 * voteId 는 Vote 의 soft delete(@Where) 영향을 받지 않도록 연관관계 없이 id 만 보관하고, 질문/선택지는 스냅샷으로 보존합니다.
 */
public class DailyVote {

    @Id
    @Column(name = "vote_date")
    private LocalDate voteDate;

    @Column(name = "vote_id")
    private Long voteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DailyVoteSource source;

    @Column(columnDefinition = "TEXT")
    private String questionSnapshot;

    private String optionASnapshot;

    private String optionBSnapshot;

    private Long optionAId;

    private Long optionBId;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private DailyVoteWinner winner;

    private Integer settledACount;

    private Integer settledBCount;

    private LocalDateTime settledAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * 게임을 지정(또는 교체)하며 voteId 와 질문/선택지 스냅샷을 갱신합니다. 선택지는 label A, B 순으로 사용합니다.
     */
    public void assign(Vote vote, DailyVoteSource source) {
        VoteOption a = findOption(vote, VoteLabel.A);
        VoteOption b = findOption(vote, VoteLabel.B);
        this.voteId = vote.getId();
        this.source = source;
        this.questionSnapshot = vote.getTitle();
        this.optionASnapshot = a.getContent();
        this.optionBSnapshot = b.getContent();
        this.optionAId = a.getId();
        this.optionBId = b.getId();
    }

    /**
     * 정산 결과(A/B 표 수, 승자, 정산 시각)를 기록합니다.
     */
    public void settle(int a, int b, DailyVoteWinner winner, LocalDateTime now) {
        this.settledACount = a;
        this.settledBCount = b;
        this.winner = winner;
        this.settledAt = now;
    }

    private static VoteOption findOption(Vote vote, VoteLabel label) {
        return vote.getVoteOptions().stream()
                .filter(o -> o.getLabel() == label)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("선택지 " + label + " 가 없는 게임입니다. voteId=" + vote.getId()));
    }
}
