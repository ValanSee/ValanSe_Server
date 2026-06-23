package com.valanse.valanse.common.message;

/**
 * VoteErrorMessage 도메인에서 사용하는 고정 선택 값을 정의하는 enum 코드입니다.
 */
public enum VoteErrorMessage {
    CATEGORY_REQUIRED("카테고리를 입력해주세요."),
    HOT_ISSUE_VOTE_NOT_FOUND("핫이슈 투표를 찾을 수 없습니다."),
    TRENDING_VOTE_NOT_FOUND("인기 급상승 투표를 찾을 수 없습니다."),
    VOTE_NOT_FOUND("투표가 존재하지 않습니다."),
    VOTE_DETAIL_NOT_FOUND("투표를 찾을 수 없습니다."),
    VOTE_OPTION_NOT_FOUND("투표 선택지가 존재하지 않습니다."),
    VOTE_OPTION_NOT_BELONG_TO_VOTE("해당 투표의 선택지가 아닙니다."),
    VOTE_TITLE_INVALID("투표 제목은 1자 이상 25자 이하여야 합니다 (공백 제외)."),
    VOTE_OPTION_COUNT_INVALID("투표 옵션은 1개 이상 4개 이하여야 합니다."),
    VOTE_OPTION_IMAGE_NOT_FOUND("투표 옵션 이미지 파일을 찾을 수 없습니다."),
    VOTE_OPTION_IMAGE_KEY_DUPLICATED("투표 옵션 이미지 키는 중복될 수 없습니다."),
    SIZE_INVALID("size는 1 이상이어야 합니다."),
    CATEGORY_INVALID("category는 ALL, FOOD, LOVE, BUY, SPORT, WORRY, ETC 중 하나여야 합니다."),
    SORT_INVALID("sort는 latest 또는 popular 중 하나여야 합니다."),
    CURSOR_INVALID("cursor 형식이 올바르지 않습니다."),
    DELETE_PERMISSION_DENIED("삭제 권한이 없습니다."),
    PERMISSION_DENIED("권한이 없습니다."),
    POST_NOT_FOUND("게시물이 존재하지 않습니다.");

    private final String message;

    VoteErrorMessage(String message) {
        this.message = message;
    }

    /**
     * VoteErrorMessage의 message 기능을 수행하는 메서드입니다.
     */
    public String message() {
        return message;
    }
}
