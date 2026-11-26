package com.valanse.valanse.common.message;

/**
 * 오늘의 밸런스 도메인에서 사용하는 고정 메시지를 정의하는 enum 코드입니다.
 */
public enum DailyVoteErrorMessage {
    DAILY_VOTE_NOT_FOUND("해당 날짜의 오늘의 밸런스를 찾을 수 없습니다."),
    DAILY_VOTE_DATE_INVALID("날짜 형식은 yyyy-MM-dd 여야 합니다."),
    DAILY_VOTE_SIZE_INVALID("size는 1 이상 50 이하여야 합니다."),
    DAILY_VOTE_RANGE_INVALID("from/to 는 필수이며 from 이 to 보다 늦을 수 없고 범위는 최대 62일입니다."),
    PAST_OR_TODAY_NOT_EDITABLE("오늘 및 지난 날짜는 변경할 수 없습니다. 내일 이후만 가능합니다."),
    OPTION_COUNT_MUST_BE_TWO("선택지가 정확히 2개(A, B)인 게임만 예약할 수 있습니다."),
    VOTE_NOT_FOUND("게임을 찾을 수 없습니다."),
    NOT_RESERVED("해당 날짜에 예약된 게임이 없습니다.");

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
