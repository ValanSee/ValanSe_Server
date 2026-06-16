package com.valanse.valanse.common.message;

/**
 * ReportErrorMessage 도메인에서 사용하는 고정 선택 값을 정의하는 enum 코드입니다.
 */
public enum ReportErrorMessage {
    COMMENT_NOT_FOUND("댓글을 찾을 수 없습니다."),
    OWN_VOTE_REPORT_NOT_ALLOWED("자신의 투표는 신고할 수 없습니다."),
    OWN_COMMENT_REPORT_NOT_ALLOWED("자신의 댓글은 신고할 수 없습니다."),
    ALREADY_REPORTED("이미 신고한 대상입니다."),
    REPORTED_VOTE_NOT_FOUND("해당 밸런스게임을 찾을 수 없습니다."),
    REPORTED_COMMENT_NOT_FOUND("해당 댓글을 찾을 수 없습니다.");

    private final String message;

    ReportErrorMessage(String message) {
        this.message = message;
    }

    /**
     * ReportErrorMessage의 message 기능을 수행하는 메서드입니다.
     */
    public String message() {
        return message;
    }
}
