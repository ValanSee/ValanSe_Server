package com.valanse.valanse.dto.MemberProfile;

public record MemberProfileResponse(
        Info profile
) {
    public record Info(
            String nickname,
            String gender,
            String age,
            String mbtiIe,
            String mbtiTf,
            String mbti
    ) {}
}
