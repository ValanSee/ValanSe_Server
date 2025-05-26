package com.valanse.valanse.domain.enums;

public enum Age {
    TEN("10"),
    TWENTY("20"),
    THIRTY("30"),
    FORTY("40");

    private final String value;

    Age(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}

