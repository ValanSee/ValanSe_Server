package com.valanse.valanse.dto.MemberBlock;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 차단/차단 해제 요청의 결과(현재 차단 여부)를 담는 응답 DTO 코드입니다.
 */
public record MemberBlockStatusResponse(
        @Schema(description = "요청 처리 후 차단 상태", example = "true")
        boolean blocked
) {
}
