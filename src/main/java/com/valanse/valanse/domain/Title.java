package com.valanse.valanse.domain;

import com.valanse.valanse.domain.common.BaseEntity;
import com.valanse.valanse.domain.enums.TitleAcquisitionType;
import com.valanse.valanse.domain.enums.TitleTier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Entity
/**
 * Title 정보를 저장하고 연관관계를 표현하는 JPA 도메인 엔티티 코드입니다.
 */
public class Title extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    private String description;

    @Builder.Default
    private long price = 0L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TitleTier tier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TitleAcquisitionType acquisitionType;

    private String requirementText;

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private int displayOrder = 0;

    /**
     * isDefaultTitle 조건을 판별하는 메서드입니다.
     */
    public boolean isDefaultTitle() {
        return acquisitionType == TitleAcquisitionType.DEFAULT;
    }

    /**
     * isPointPurchaseTitle 조건을 판별하는 메서드입니다.
     */
    public boolean isPointPurchaseTitle() {
        return acquisitionType == TitleAcquisitionType.POINT_PURCHASE;
    }

    /**
     * isPurchasable 조건을 판별하는 메서드입니다.
     */
    public boolean isPurchasable(long point) {
        return active && isPointPurchaseTitle() && point >= price;
    }

    /**
     * Title의 deactivate 기능을 수행하는 메서드입니다.
     */
    public void deactivate() {
        this.active = false;
    }

    /**
     * Title 데이터를 수정하는 메서드입니다.
     */
    public void update(
            String code,
            String name,
            String description,
            long price,
            TitleTier tier,
            TitleAcquisitionType acquisitionType,
            String requirementText,
            boolean active,
            int displayOrder
    ) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.price = price;
        this.tier = tier;
        this.acquisitionType = acquisitionType;
        this.requirementText = requirementText;
        this.active = active;
        this.displayOrder = displayOrder;
    }
}
