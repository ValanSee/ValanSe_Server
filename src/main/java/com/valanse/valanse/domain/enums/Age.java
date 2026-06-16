package com.valanse.valanse.domain.enums;

/**
 * Age 도메인에서 사용하는 고정 선택 값을 정의하는 enum 코드입니다.
 */
public enum Age {
    TEN("10"),
    TWENTY("20"),
    THIRTY("30"),
    OVER_FORTY("40+");

    private final String value;

    Age(String value) {
        this.value = value;
    }

    /**
     * Value 정보를 조회하는 메서드입니다.
     */
    public String getValue() {
        return value;
    }
}

