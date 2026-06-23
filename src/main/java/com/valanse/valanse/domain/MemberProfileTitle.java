package com.valanse.valanse.domain;

import com.valanse.valanse.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "uk_member_profile_title_profile_title", columnNames = {"member_profile_id", "title_id"})
})
/**
 * MemberProfileTitle 정보를 저장하고 연관관계를 표현하는 JPA 도메인 엔티티 코드입니다.
 */
public class MemberProfileTitle extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_profile_id", nullable = false)
    private MemberProfile memberProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "title_id", nullable = false)
    private Title title;

    @Builder.Default
    @Column(nullable = false)
    private boolean equipped = false;

    private LocalDateTime acquiredAt;

    /**
     * MemberProfileTitle의 prePersist 기능을 수행하는 메서드입니다.
     */
    @PrePersist
    public void prePersist() {
        if (acquiredAt == null) {
            acquiredAt = LocalDateTime.now();
        }
    }

    /**
     * MemberProfileTitle의 equip 기능을 수행하는 메서드입니다.
     */
    public void equip() {
        this.equipped = true;
    }

    /**
     * MemberProfileTitle의 unequip 기능을 수행하는 메서드입니다.
     */
    public void unequip() {
        this.equipped = false;
    }
}
