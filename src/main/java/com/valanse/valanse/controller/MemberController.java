package com.valanse.valanse.controller;

import com.valanse.valanse.common.auth.JwtTokenProvider;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.dto.AccessTokenDto;
import com.valanse.valanse.dto.KakaoProfileDto;
import com.valanse.valanse.dto.RedirectDto;
import com.valanse.valanse.service.KakaoService;
import com.valanse.valanse.service.MemberProfileService;
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
            description = "프론트엔드에서 인가코드(code)를 받아서 카카오 로그인을 처리합니다. 만약 회원이 존재하지 않으면 자동 회원가입 로직 수행 후 JWT 토큰을 발급하여 반환합니다. Authorize 버튼 클릭 후 해당 토큰을 Bearer 없이 붙여넣어서 인증해주시면 됩니다."
    )
    @PostMapping("/kakao/login")
    public ResponseEntity<?> kakaoLogin(@RequestBody RedirectDto redirectDto) {
        AccessTokenDto accessTokenDto = kakaoService.getAccessToken(redirectDto.getCode()); // 인가 코드 넘겨주기

        KakaoProfileDto kakaoProfileDto = kakaoService.getKakaoProfile(accessTokenDto.getAccess_token());

        Member originalMember = memberService.getMemberBySocialId(kakaoProfileDto.getId());

        if (originalMember == null) {
            originalMember = memberService.createOauth(kakaoProfileDto.getId(), kakaoProfileDto.getKakao_account().getEmail(), kakaoProfileDto.getKakao_account().getProfile().getNickname(), kakaoProfileDto.getKakao_account().getProfile().getProfile_image_url());
        }
        String jwtToken = jwtTokenProvider.createToken(originalMember.getEmail(), originalMember.getRole().toString());

        Map<String, Object> loginInfo = new HashMap<>();
        loginInfo.put("id", originalMember.getId());
        loginInfo.put("token", jwtToken);
        return new ResponseEntity<>(loginInfo, HttpStatus.OK);
    }


    @Operation(summary = "추가 프로필 정보 유무 확인", description = "추가 정보 입력이 필요한지 확인하고, 그 결과를 boolean 값으로 반환합니다.")
    @GetMapping("/profile-check")
    public boolean checkAdditionalInfo() {
        return memberProfileService.hasProfileAdditionalInfo();
    }

}