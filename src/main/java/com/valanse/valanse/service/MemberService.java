package com.valanse.valanse.service;

import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.MemberProfile;
import com.valanse.valanse.repository.MemberProfileRepository;
import com.valanse.valanse.repository.MemberRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final MemberProfileRepository memberProfileRepository;

    public Member getMemberBySocialId(String socialId) {
        Member member = memberRepository.findBySocialId(socialId).orElse(null);
        return member;
    }

    public Member createOauth(String socialId, String email, String name, String profile_image_url) {
        Member member = Member.builder()
                .email(email)
                .socialId(socialId)
                .name(name)
                .build();
        memberRepository.save(member);
        MemberProfile memberProfile = MemberProfile.builder()
                .member(member)
                .profile_image_url(profile_image_url)
                .build();
        memberProfileRepository.save(memberProfile);
        return member;
    }
}

