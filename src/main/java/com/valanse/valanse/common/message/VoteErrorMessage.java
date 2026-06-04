package com.valanse.valanse.common.message;

public enum VoteErrorMessage {
    VOTE_NOT_FOUND("투표가 존재하지 않습니다.");

    private final String message;

    VoteErrorMessage(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }
}
