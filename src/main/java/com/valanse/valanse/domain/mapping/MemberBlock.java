package com.valanse.valanse.domain.mapping;

import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "member_block",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_member_block_blocker_blocked", columnNames = {"blocker_id", "blocked_id"})
        },
        indexes = {
                @Index(name = "idx_member_block_blocked", columnList = "blocked_id")
        }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
/**
 * 회원 차단(blocker가 blocked를 차단) 관계를 저장하는 JPA 도메인 엔티티 코드입니다.
 * 차단한 회원에게는 차단된 회원이 작성한 투표/댓글이 목록에서 노출되지 않습니다.
 */
public class MemberBlock extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocker_id", nullable = false)
    private Member blocker;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocked_id", nullable = false)
    private Member blocked;
}
