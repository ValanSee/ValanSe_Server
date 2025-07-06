package com.valanse.valanse.service.MemberProfileService;


import com.valanse.valanse.dto.MemberProfile.MemberMyPageResponse;
import com.valanse.valanse.dto.MemberProfile.MemberProfileRequest;
import com.valanse.valanse.dto.MemberProfile.MemberProfileResponse;

public interface MemberProfileService {
    void saveOrUpdateProfile(MemberProfileRequest dto);
    MemberProfileResponse getProfile();
    MemberMyPageResponse getMyProfile();

    boolean isAvailableNickname(String nickname);      // 중복 아님 → true
    boolean isMeaningfulNickname(String nickname);     // 무의미하지 않음 → true
    boolean isCleanNickname(String nickname);          // 비속어 아님 → true
}

