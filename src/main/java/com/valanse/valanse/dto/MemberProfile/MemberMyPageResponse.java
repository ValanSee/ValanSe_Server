package com.valanse.valanse.dto.MemberProfile;

/**
 * MemberMyPageResponse API 응답 또는 계층 간 전달 값을 담는 DTO 코드입니다.
 */
public record MemberMyPageResponse(
        MyPageInfo profile
) {
    /**
     * API 요청과 응답 데이터를 전달하기 위한 DTO 코드입니다.
     */
    public record MyPageInfo(
            String profile_image_url,
            String kakaoname,
            String email,
            String nickname,
            String gender,
            String age,
            String mbti,
            String title,
            long point
    ){}
}
