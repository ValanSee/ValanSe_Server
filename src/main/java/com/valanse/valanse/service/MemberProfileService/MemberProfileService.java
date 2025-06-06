package com.valanse.valanse.service.MemberProfileService;


import com.valanse.valanse.dto.MemberProfile.MemberMyPageResponse;
import com.valanse.valanse.dto.MemberProfile.MemberProfileRequest;
import com.valanse.valanse.dto.MemberProfile.MemberProfileResponse;

public interface MemberProfileService {
    void saveOrUpdateProfile(MemberProfileRequest dto);
    MemberProfileResponse getProfile();
    MemberMyPageResponse getMyProfile();
}
