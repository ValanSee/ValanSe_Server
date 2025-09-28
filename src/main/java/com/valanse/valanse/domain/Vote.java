package com.valanse.valanse.domain;

import com.valanse.valanse.domain.common.BaseEntity;
import com.valanse.valanse.domain.enums.VoteCategory;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.Where;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Where(clause = "deleted_at IS NULL")
public class Vote extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private VoteCategory category; // 예: 연애, 음식, 기타

    private String title;

    @Column(columnDefinition = "TEXT")
    private String content; // 투표 상세 내용 (nullable)

    @Builder.Default
    private Integer totalVoteCount = 0; //Builder.Default 설정이 없으면 null값이 기본값이라 오류가 발생한다.

    // 반응성 관련 필드들
    @Builder.Default
    private Integer reactivityScore = 0; // 계산된 반응성 점수 (투표수 + 댓글수 + 공유수)

    private LocalDateTime reactivityUpdatedAt;// 반응성 마지막 계산 시점


    @Builder.Default
    @OneToMany(mappedBy = "vote", cascade = CascadeType.ALL)
    private List<VoteOption> voteOptions = new ArrayList<>();

    @OneToOne(mappedBy = "vote", cascade = CascadeType.ALL)
    private CommentGroup commentGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // totalVoteCount에 대한 Setter 추가
    public void setTotalVoteCount(Integer totalVoteCount) {
        this.totalVoteCount = totalVoteCount;
    }

    // 추가 : 반응성 점수 업데이트 메서드
    public void updateReactivityScore(){
        Integer commentCount = (commentGroup != null) ? commentGroup.getTotalCommentCount() : 0;
        Integer shareCount = 0; // 향후 확장용

        this.reactivityScore = this.totalVoteCount + commentCount + shareCount;
        this.reactivityUpdatedAt = LocalDateTime.now();
    }

    // 편의 메서드: VoteOption 추가
    public void addVoteOption(VoteOption voteOption) {
        this.voteOptions.add(voteOption);
        voteOption.setVote(this);
    }

}

