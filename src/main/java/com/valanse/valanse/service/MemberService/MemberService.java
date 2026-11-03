package com.valanse.valanse.service.MemberService;

import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.enums.SocialType;

/**
 * MemberService 기능의 비즈니스 계약을 정의하는 서비스 인터페이스 코드입니다.
 */
public interface MemberService {

    Member getMemberBySocial(SocialType socialType, String socialId);
    Member createOauth(String socialId, String email, String name, String profile_image_url, String access_token, String refresh_token);
    Member createAppleMember(String appleUserId, String email, String name, String appleRefreshToken, String appleClientId);
    Member deleteMemberById();
    Member findById(Long id);
}
