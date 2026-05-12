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

    private String colorHex;

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private int displayOrder = 0;

    public boolean isDefaultTitle() {
        return acquisitionType == TitleAcquisitionType.DEFAULT;
    }

    public boolean isPointPurchaseTitle() {
        return acquisitionType == TitleAcquisitionType.POINT_PURCHASE;
    }

    public boolean isPurchasable(long point) {
        return active && isPointPurchaseTitle() && point >= price;
    }

    public void deactivate() {
        this.active = false;
    }
}
