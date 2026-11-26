package com.valanse.valanse.common.message;

/**
 * 오늘의 밸런스 도메인에서 사용하는 고정 메시지를 정의하는 enum 코드입니다.
 */
public enum DailyVoteErrorMessage {
    DAILY_VOTE_NOT_FOUND("해당 날짜의 오늘의 밸런스를 찾을 수 없습니다."),
    DAILY_VOTE_DATE_INVALID("날짜 형식은 yyyy-MM-dd 여야 합니다."),
    DAILY_VOTE_SIZE_INVALID("size는 1 이상 50 이하여야 합니다.");

    private final String message;

    DailyVoteErrorMessage(String message) {
        this.message = message;
    }

    /**
     * DailyVoteErrorMessage의 message 기능을 수행하는 메서드입니다.
     */
    public String message() {
        return message;
    }
}
