package com.valanse.valanse.controller;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.common.auth.JwtTokenProvider;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.dto.Login.AccessTokenDto;
import com.valanse.valanse.dto.Login.KakaoProfileDto;
import com.valanse.valanse.dto.MemberProfile.MemberProfileRequest;
import com.valanse.valanse.dto.MemberProfile.MemberProfileResponse;
import com.valanse.valanse.dto.Login.RedirectDto;
import com.valanse.valanse.service.KakaoService;
import com.valanse.valanse.service.MemberProfileService.MemberProfileService;
import com.valanse.valanse.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "회원 API", description = "회원 관련 기능 (로그인, 로그아웃, 프로필 조회 등)")
@RestController
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final JwtTokenProvider jwtTokenProvider;
    private final KakaoService kakaoService;
    private final MemberProfileService memberProfileService;

    @Operation(
            summary = "카카오 로그인",
            description = "프론트엔드에서 인가코드(code)를 받아서 카카오 로그인을 처리합니다. 만약 회원이 존재하지 않으면 자동 회원가입 로직 수행 후 JWT 토큰을 발급하여 반환합니다."
    )
    @PostMapping("/kakao/login")
    public ResponseEntity<?> kakaoLogin(@RequestBody RedirectDto redirectDto) {
        AccessTokenDto accessTokenDto = kakaoService.getAccessToken(redirectDto.getCode());
        if (accessTokenDto == null || accessTokenDto.getAccess_token() == null) {
            throw new ApiException("AccessToken 발급 실패", HttpStatus.UNAUTHORIZED);
        }

        KakaoProfileDto kakaoProfileDto = kakaoService.getKakaoProfile(accessTokenDto.getAccess_token());
        if (kakaoProfileDto == null || kakaoProfileDto.getId() == null) {
            throw new ApiException("카카오 사용자 정보 조회 실패", HttpStatus.UNAUTHORIZED);
        }

        Member originalMember = memberService.getMemberBySocialId(kakaoProfileDto.getId());
        if (originalMember == null) {
            originalMember = memberService.createOauth(
                    kakaoProfileDto.getId(),
                    kakaoProfileDto.getKakao_account().getEmail(),
                    kakaoProfileDto.getKakao_account().getProfile().getNickname(),
                    kakaoProfileDto.getKakao_account().getProfile().getProfile_image_url()
            );
        }

        String jwtToken = jwtTokenProvider.createToken(
                originalMember.getEmail(),
                originalMember.getRole().toString()
        );

        Map<String, Object> loginInfo = new HashMap<>();
        loginInfo.put("id", originalMember.getId());
        loginInfo.put("token", jwtToken);

        return ResponseEntity.ok(loginInfo);
    }

    @Operation(
            summary = "회원 프로필 정보 저장",
            description = "닉네임, 성별, 나이, MBTI 정보를 저장하거나 수정합니다. 모든 필드가 채워진 경우에만 저장됩니다. 만약 아직 프로필 정보가 없는 경우 null 을 반환하니 식별에 사용하시면 됩니다."
    )
    @PostMapping("/profile")
    public ResponseEntity<Void> saveProfile(@RequestBody MemberProfileRequest dto) {
        memberProfileService.saveOrUpdateProfile(dto);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "회원 프로필 정보 조회",
            description = "현재 로그인한 회원의 프로필 정보를 조회합니다. 정보가 없으면 'profile: null' 형태로 반환됩니다."
    )
    @GetMapping("/profile")
    public ResponseEntity<MemberProfileResponse> getProfile() {
        MemberProfileResponse response = memberProfileService.getProfile();
        return ResponseEntity.ok(response);
    }
}
