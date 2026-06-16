package com.valanse.valanse.dto.MemberProfile;

import com.valanse.valanse.domain.enums.Role;

/**
 * MemberProfileResponse API 응답 또는 계층 간 전달 값을 담는 DTO 코드입니다.
 */
public record MemberProfileResponse(
        Info profile
) {
    /**
     * API 요청과 응답 데이터를 전달하기 위한 DTO 코드입니다.
     */
    public record Info(
            String nickname,
            String gender,
            String age,
            String mbtiIe,
            String mbtiTf,
            String mbti,
            Role role,
            String title,
            long point
    ) {}
}
