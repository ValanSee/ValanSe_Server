package com.valanse.valanse.service;

import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.MemberProfile;
import com.valanse.valanse.repository.MemberProfileRepository;
import com.valanse.valanse.repository.MemberRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;

    public Member getMemberBySocialId(String socialId) {
        Member member = memberRepository.findBySocialId(socialId).orElse(null);
        return member;
    }

    public Member createOauth(String socialId, String email, String name, String profile_image_url, String access_token, String refresh_token) {
        Member member = Member.builder()
                .email(email)
                .socialId(socialId)
                .name(name)
                .profile_image_url(profile_image_url)
                .kakaoAccessToken(access_token)
                .kakaoRefreshToken(refresh_token)
                .build();
        memberRepository.save(member);
        return member;
    }

    public Member deleteMemberById() {
        Long userId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
        Member member = memberRepository.findById(userId).orElse(null);
        memberRepository.delete(member); // TODO: soft delete 방식 설정하기
        return member;
    }
}

