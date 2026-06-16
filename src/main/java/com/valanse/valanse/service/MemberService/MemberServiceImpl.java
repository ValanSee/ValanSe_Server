package com.valanse.valanse.service.MemberService;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.common.message.MemberErrorMessage;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.enums.PointType;
import com.valanse.valanse.repository.MemberRepository;
import com.valanse.valanse.repository.MemberProfileRepository;
import com.valanse.valanse.service.PointService.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
/**
 * OAuth 회원 생성, 회원 조회, 회원 탈퇴 처리를 담당하는 서비스 코드입니다.
 */
public class MemberServiceImpl implements MemberService {
    private final MemberRepository memberRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final PointService pointService;

    /**
     * MemberBySocialId 정보를 조회하는 메서드입니다.
     */
    @Transactional(readOnly = true)
    @Override
    public Member getMemberBySocialId(String socialId) {
        Member member = memberRepository.findBySocialIdAndDeletedAtIsNull(socialId).orElse(null);
        return member;
    }

    /**
     * Oauth 데이터를 생성하는 메서드입니다.
     */
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

        // 회원가입 포인트 지급 (프로필 생성 후 지급되므로 여기선 기록만 남김)
        // 실제 포인트는 프로필 저장 시점에 지급 (MemberProfileServiceImpl 참고)

        return member;
    }

    /**
     * MemberById 데이터를 삭제하는 메서드입니다.
     */
    @Override
    public Member deleteMemberById() {
        Long userId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
        Member member = memberRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException(MemberErrorMessage.MEMBER_NOT_FOUND.message(), HttpStatus.NOT_FOUND));

        member.softDelete(); // Soft delete 처리

        // MemberProfile도 함께 soft delete (닉네임 중복 방지)
        memberProfileRepository.findByMemberId(userId)
                .ifPresent(profile -> profile.softDelete());

        return memberRepository.save(member); // 삭제된 상태로 저장
    }

    //  추가된 메서드
    /**
     * ById 조건에 맞는 데이터를 찾는 메서드입니다.
     */
    @Transactional(readOnly = true)
    @Override
    public Member findById(Long id) {
        return memberRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ApiException(MemberErrorMessage.MEMBER_NOT_FOUND.message(), HttpStatus.NOT_FOUND));
    }
}
