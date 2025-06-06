package com.valanse.valanse.dto.MemberProfile;

public record MemberMyPageResponse(
        MyPageInfo profile
) {
    public record MyPageInfo(
            String profile_image_url,
            String kakaoname,
            String email,
            String nickname,
            String gender,
            String age,
            String mbti
    ){}
}


