package com.valanse.valanse.service.MemberProfileService;

import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.MemberProfile;
import com.valanse.valanse.dto.MemberProfile.MemberMyPageResponse;
import com.valanse.valanse.dto.MemberProfile.MemberProfileRequest;
import com.valanse.valanse.dto.MemberProfile.MemberProfileResponse;
import com.valanse.valanse.repository.MemberProfileRepository;
import com.valanse.valanse.repository.MemberRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberProfileServiceImpl implements MemberProfileService {

    private final MemberRepository memberRepository;
    private final MemberProfileRepository memberProfileRepository;

    @Override
    public void saveOrUpdateProfile(MemberProfileRequest dto) {
        Long userId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());

        Member member = memberRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

        MemberProfile profile = memberProfileRepository.findByMemberId(userId)
                .orElse(new MemberProfile());

        profile = MemberProfile.builder()
                .member(member)
                .nickname(dto.nickname())
                .gender(dto.gender())
                .age(dto.age())
                .mbtiIe(dto.mbtiIe())
                .mbtiTf(dto.mbtiTf())
                .mbti(dto.mbti())
                .build();

        memberProfileRepository.save(profile);
    }

    @Override
    public MemberProfileResponse getProfile() {
        Long userId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());

        MemberProfile profile = memberProfileRepository.findByMemberId(userId)
                .orElse(null);

        if (profile == null) {
            return new MemberProfileResponse(null);
        }

        MemberProfileResponse.Info info = new MemberProfileResponse.Info(
                profile.getNickname(),
                profile.getGender() != null ? profile.getGender().name() : null,
                profile.getAge() != null ? profile.getAge().name() : null,
                profile.getMbtiIe() != null ? profile.getMbtiIe().name() : null,
                profile.getMbtiTf() != null ? profile.getMbtiTf().name() : null,
                profile.getMbti() != null ? profile.getMbti() : null
        );

        return new MemberProfileResponse(info);
    }

    @Override
    public MemberMyPageResponse getMyProfile() {
        Long userId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());

        MemberProfile profile = memberProfileRepository.findByMemberId(userId)
                .orElse(null);
        Member member = memberRepository.findByIdAndDeletedAtIsNull(userId).orElse(null);

        if (profile == null) {
            return new MemberMyPageResponse(null);
        }

        MemberMyPageResponse.MyPageInfo info = new MemberMyPageResponse.MyPageInfo(
                member.getProfile_image_url(),
                member.getName(),
                member.getEmail(),
                profile.getNickname(),
                profile.getGender() != null ? profile.getGender().name() : null,
                profile.getAge() != null ? profile.getAge().name() : null,
                profile.getMbti() != null ? profile.getMbti() : null
        );

        return new MemberMyPageResponse(info);
    }
}
