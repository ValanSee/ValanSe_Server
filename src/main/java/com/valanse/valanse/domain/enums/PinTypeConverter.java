package com.valanse.valanse.domain.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * PinType.NONE을 DB의 NULL로 저장해 pin_type에 "고정 종류별 최대 한 행" UNIQUE
 * 제약을 걸 수 있게 하는 컨버터입니다. MySQL/H2는 UNIQUE 컬럼에 NULL을 여러 개
 * 허용하므로 고정되지 않은 투표(NONE)는 제약의 영향을 받지 않습니다.
 */
@Converter
public class PinTypeConverter implements AttributeConverter<PinType, String> {

    @Override
    public String convertToDatabaseColumn(PinType attribute) {
        return (attribute == null || attribute == PinType.NONE) ? null : attribute.name();
    }

    @Override
    public PinType convertToEntityAttribute(String dbData) {
        return dbData == null ? PinType.NONE : PinType.valueOf(dbData);
    }
}
