package com.valanse.valanse.service;

import com.valanse.valanse.domain.MemberProfile;
import com.valanse.valanse.repository.MemberProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContextHolder;


@Service
@RequiredArgsConstructor
public class MemberProfileService {

    private final MemberProfileRepository memberProfileRepository;

    public boolean hasProfileAdditionalInfo() {
        // 인증된 사용자의 이메일 꺼내기
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        // 이메일로 MemberProfile 조회
        MemberProfile profile = memberProfileRepository.findByMemberEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("프로필 정보가 없습니다."));

        // 추가 정보 여부 확인
        boolean hasInfo = profile.getNickname() != null ||
                profile.getGender() != null ||
                profile.getAge() != null ||
                profile.getMbtiIe() != null ||
                profile.getMbtiTf() != null;

        return hasInfo;
    }
}
