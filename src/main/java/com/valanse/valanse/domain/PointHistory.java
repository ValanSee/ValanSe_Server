package com.valanse.valanse.domain;

import com.valanse.valanse.domain.enums.PointType;
import jakarta.persistence.*;
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
public class PointHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;

    @Enumerated(EnumType.STRING)
    private PointType type;

    private Long amount;

    private LocalDateTime createdAt;

    public static PointHistory of(
            Member member,
            PointType type,
            Long amount
    ) {
        return new PointHistory(member, type, amount);
    }

    private PointHistory(Member member, PointType type, Long amount) {
        this.member = member;
        this.type = type;
        this.amount = amount;
        this.createdAt = LocalDateTime.now();
    }
}
