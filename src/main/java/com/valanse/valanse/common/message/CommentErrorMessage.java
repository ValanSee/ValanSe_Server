package com.valanse.valanse.common.message;

/**
 * CommentErrorMessage 도메인에서 사용하는 고정 선택 값을 정의하는 enum 코드입니다.
 */
public enum CommentErrorMessage {
    COMMENT_NOT_FOUND("댓글이 존재하지 않습니다."),
    PARENT_COMMENT_NOT_FOUND("부모 댓글이 존재하지 않습니다."),
    WRONG_SORT_PARAMETER("sort 파라미터는 'latest' 또는 'oldest'만 허용됩니다.");

    private final String message;

    CommentErrorMessage(String message) {
        this.message = message;
    }

    /**
     * CommentErrorMessage의 message 기능을 수행하는 메서드입니다.
     */
    public String message() {
        return message;
    }
}
