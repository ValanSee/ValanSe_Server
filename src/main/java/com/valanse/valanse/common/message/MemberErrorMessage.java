package com.valanse.valanse.common.message;

/**
 * MemberErrorMessage 도메인에서 사용하는 고정 선택 값을 정의하는 enum 코드입니다.
 */
public enum MemberErrorMessage {
    MEMBER_NOT_FOUND("회원이 존재하지 않습니다."),
    NICKNAME_REQUIRED("닉네임을 입력해주세요"),
    NICKNAME_INVALID_FORMAT("닉네임 형식이 올바르지 않습니다."),
    NICKNAME_NOT_CLEAN("사용할 수 없는 닉네임입니다."),
    GENDER_REQUIRED("성별을 선택해주세요"),
    AGE_REQUIRED("나이를 선택해주세요"),
    MBTI_REQUIRED("MBTI를 모두 선택해주세요"),
    MBTI_INVALID_LENGTH("MBTI는 4자리여야 합니다 (예: ENFP)"),
    NICKNAME_DUPLICATED("이미 사용 중인 닉네임입니다.");

    private final String message;

    MemberErrorMessage(String message) {
        this.message = message;
    }

    /**
     * MemberErrorMessage의 message 기능을 수행하는 메서드입니다.
     */
    public String message() {
        return message;
    }
}
