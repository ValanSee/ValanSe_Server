package com.valanse.valanse.domain.enums;

/**
 * ReportReason 신고 사유 카테고리를 정의하는 enum 코드입니다.
 */
public enum ReportReason {
    SPAM("스팸"),
    SEXUAL_CONTENT("성적 콘텐츠"),
    HATE_OR_HARASSMENT("혐오 또는 괴롭힘"),
    VIOLENCE_OR_THREAT("폭력 또는 위협"),
    ILLEGAL_OR_HARMFUL("불법 또는 유해 콘텐츠"),
    PERSONAL_INFORMATION("개인정보 노출"),
    ETC("기타");

    private final String description;

    ReportReason(String description) {
        this.description = description;
    }

    /**
     * ReportReason의 description 기능을 수행하는 메서드입니다.
     */
    public String description() {
        return description;
    }
}
