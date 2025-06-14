package com.valanse.valanse.service.MemberService;

import com.valanse.valanse.domain.Member;

public interface MemberService {

    Member getMemberBySocialId(String socialId);
    Member createOauth(String socialId, String email, String name, String profile_image_url, String access_token, String refresh_token);
    Member deleteMemberById();
}
