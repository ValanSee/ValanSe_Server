package com.valanse.valanse.common.api;

import org.springframework.http.HttpStatus;

/**
 * 외부 요청으로 전달되는 페이지 번호와 페이지 크기의 공통 상한을 검증합니다.
 */
public final class PaginationValidator {

    public static final int MAX_PAGE_SIZE = 50;

    private PaginationValidator() {
    }

    public static void validateSize(int size) {
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ApiException(
                    "size는 1 이상 " + MAX_PAGE_SIZE + " 이하여야 합니다.",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    public static void validatePageAndSize(int page, int size) {
        if (page < 0) {
            throw new ApiException("page는 0 이상이어야 합니다.", HttpStatus.BAD_REQUEST);
        }

        validateSize(size);
    }
}
