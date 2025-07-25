package com.valanse.valanse.service.MemberService;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.repository.MemberRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {
    private final MemberRepository memberRepository;

    @Override
    public Member getMemberBySocialId(String socialId) {
        Member member = memberRepository.findBySocialIdAndDeletedAtIsNull(socialId).orElse(null);
        return member;
    }

    @Override
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

    @Override
    public Member deleteMemberById() {
        Long userId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
        Member member = memberRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException("사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND));

        member.softDelete(); // Soft delete 처리
        return memberRepository.save(member); // 삭제된 상태로 저장
    }
//    @Override
//    public void deleteMemberById() {
//        Long userId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
//
//        Member member = memberRepository.findById(userId)
//                .orElseThrow(() -> new ApiException("사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND));
//
//        memberRepository.delete(member); // hard delete
//    }


    //  추가된 메서드
    @Override
    public Member findById(Long id) {
        return memberRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ApiException("사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND));
    }
}
