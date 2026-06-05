package com.valanse.valanse.common.message;

public enum TitleErrorMessage {
    PROFILE_NOT_FOUND("프로필이 존재하지 않습니다."),
    UNOWNED_TITLE("보유하지 않은 칭호입니다."),
    TITLE_NOT_EQUIPPABLE("장착할 수 없는 칭호입니다."),
    TITLE_NOT_FOUND("칭호가 존재하지 않습니다."),
    TITLE_NOT_PURCHASABLE("구매할 수 없는 칭호입니다."),
    TITLE_ALREADY_OWNED("이미 보유한 칭호입니다."),
    POINT_NOT_ENOUGH("포인트가 부족합니다. (필요포인트 %dP 필요)"),
    TITLE_CODE_DUPLICATED("이미 존재하는 칭호 코드입니다."),
    TITLE_ALREADY_DELETED("이미 삭제된 칭호입니다."),
    DEFAULT_TITLE_DELETE_NOT_ALLOWED("기본 칭호는 삭제할 수 없습니다."),
    DEFAULT_TITLE_NOT_FOUND("기본 칭호가 존재하지 않습니다."),
    MEMBER_NOT_FOUND("회원을 찾을 수 없습니다."),
    ADMIN_ONLY("관리자만 접근 가능합니다."),
    CREATE_REQUEST_EMPTY("칭호 생성 요청이 비어있습니다."),
    CODE_REQUIRED("칭호 코드를 입력해주세요."),
    NAME_REQUIRED("칭호 이름을 입력해주세요."),
    TIER_REQUIRED("칭호 등급을 입력해주세요."),
    ACQUISITION_TYPE_REQUIRED("칭호 획득 방식을 입력해주세요."),
    PRICE_INVALID("칭호 가격은 0 이상이어야 합니다."),
    DISPLAY_ORDER_INVALID("칭호 표시 순서는 0 이상이어야 합니다."),
    UPDATE_REQUEST_EMPTY("칭호 수정 요청이 비어있습니다.");

    private final String message;

    TitleErrorMessage(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }

    public String message(Object... args) {
        return String.format(message, args);
    }
}
