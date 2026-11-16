package com.valanse.valanse.dto.MemberBlock;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 내가 차단한 회원 목록 응답 DTO 코드입니다.
 */
public record MemberBlockListResponse(
        List<BlockedMember> blocks
) {

    public record BlockedMember(
            @Schema(description = "차단한 회원 id", example = "1")
            Long memberId,
            @Schema(description = "차단한 회원 닉네임 (프로필이 없으면 null)", example = "한라봉")
            String nickname,
            @Schema(description = "차단한 시각")
            LocalDateTime blockedAt
    ) {
    }
}
