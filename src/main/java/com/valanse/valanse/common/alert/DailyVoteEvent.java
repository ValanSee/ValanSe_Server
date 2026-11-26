package com.valanse.valanse.common.alert;

import java.time.LocalDate;

/**
 * 오늘의 밸런스 선정·정산·교체·예약 취소 결과를 Discord 알림 계층에 전달하는 이벤트입니다.
 * voteId/question/source/detail 은 타입에 따라 null 일 수 있습니다.
 */
public record DailyVoteEvent(
        Type type,
        LocalDate date,
        Long voteId,
        String question,
        String source,
        String detail
) {
    public enum Type { SELECTED, NO_CANDIDATE, REPLACED, RESERVATION_CANCELLED, SETTLED }

    public static DailyVoteEvent selected(LocalDate date, Long voteId, String question, String source) {
        return new DailyVoteEvent(Type.SELECTED, date, voteId, question, source, null);
    }

    public static DailyVoteEvent noCandidate(LocalDate date) {
        return new DailyVoteEvent(Type.NO_CANDIDATE, date, null, null, null, null);
    }

    public static DailyVoteEvent replaced(LocalDate date, Long voteId, String question, String source, String detail) {
        return new DailyVoteEvent(Type.REPLACED, date, voteId, question, source, detail);
    }

    public static DailyVoteEvent reservationCancelled(LocalDate date, Long voteId, String question, String detail) {
        return new DailyVoteEvent(Type.RESERVATION_CANCELLED, date, voteId, question, null, detail);
    }

    public static DailyVoteEvent settled(LocalDate date, Long voteId, String question, String source, String detail) {
        return new DailyVoteEvent(Type.SETTLED, date, voteId, question, source, detail);
    }
}
