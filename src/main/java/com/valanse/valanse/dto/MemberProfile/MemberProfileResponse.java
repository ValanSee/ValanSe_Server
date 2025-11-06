package com.valanse.valanse.dto.MemberProfile;

import com.valanse.valanse.domain.enums.Role;

public record MemberProfileResponse(
        Info profile
) {
    public record Info(
            String nickname,
            String gender,
            String age,
            String mbtiIe,
            String mbtiTf,
            String mbti,
            Role role
    ) {}
}
