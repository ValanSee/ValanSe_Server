package com.valanse.valanse.service;

import com.valanse.valanse.domain.Member;
import com.valanse.valanse.dto.MemberCreateDto;
import com.valanse.valanse.dto.MemberLoginDto;
import com.valanse.valanse.repository.MemberRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Transactional
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Member create(MemberCreateDto memberCreateDto) {
        Member member = Member.builder()
                .email(memberCreateDto.getEmail())
                .password(passwordEncoder.encode(memberCreateDto.getPassword()))
                .build();
        memberRepository.save(member);
        return member;
    }

    public Member login(MemberLoginDto memberLoginDto) {
        Optional<Member> optMember = memberRepository.findByEmail(memberLoginDto.getEmail());
        if (!optMember.isPresent()) {
            throw new IllegalArgumentException("이메일이 존재하지 않습니다.");
        }
        Member member = optMember.get();
        if (!passwordEncoder.matches(memberLoginDto.getPassword(), member.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
        return member;
    }
}

