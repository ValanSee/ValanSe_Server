package com.valanse.valanse.service;

import com.valanse.valanse.domain.MemberProfile;
import com.valanse.valanse.repository.MemberProfileRepository;
import com.valanse.valanse.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContextHolder;


@Service
@RequiredArgsConstructor
public class MemberProfileService {

    private final MemberProfileRepository memberProfileRepository;
    private final MemberRepository memberRepository;

    public boolean hasProfileAdditionalInfo() {
        // 인증된 사용자 이메일 추출
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        // 1. Member 테이블에서 먼저 존재 확인
        memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보가 없습니다."));

        // 2. MemberProfile 조회 (없어도 예외 안 던지고 null로 받기)
        MemberProfile profile = memberProfileRepository.findByMemberEmail(email)
                .orElse(null);

        if (profile == null) {
            return false; // 프로필 자체가 없으면 false
        }

        // 3. 프로필 항목이 하나만 null인 경우에도 false (도중에 입력 중단한 경우 등)
        boolean hasInfo = profile.getNickname() != null &&
                profile.getGender() != null &&
                profile.getAge() != null &&
                profile.getMbtiIe() != null &&
                profile.getMbtiTf() != null;

        return hasInfo;
    }
}
