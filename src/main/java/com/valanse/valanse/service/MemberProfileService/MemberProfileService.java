package com.valanse.valanse.service.MemberProfileService;


import com.valanse.valanse.dto.MemberProfile.MemberMyPageResponse;
import com.valanse.valanse.dto.MemberProfile.MemberProfileImageResponse;
import com.valanse.valanse.dto.MemberProfile.MemberProfileRequest;
import com.valanse.valanse.dto.MemberProfile.MemberProfileResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * MemberProfileService 기능의 비즈니스 계약을 정의하는 서비스 인터페이스 코드입니다.
 */
public interface MemberProfileService {
    void saveOrUpdateProfile(MemberProfileRequest dto);
    MemberProfileImageResponse updateProfileImage(MultipartFile file);
    MemberProfileResponse getProfile();
    MemberMyPageResponse getMyProfile();

    boolean isAvailableNickname(String nickname);
    boolean isMeaningfulNickname(String nickname);
    boolean isCleanNickname(String nickname);
}
