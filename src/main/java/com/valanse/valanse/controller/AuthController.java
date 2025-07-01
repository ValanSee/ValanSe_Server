package com.valanse.valanse.controller;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.common.auth.JwtTokenProvider;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.dto.Login.AccessTokenDto;
import com.valanse.valanse.dto.Login.KakaoProfileDto;
import com.valanse.valanse.dto.Login.RedirectDto;
import com.valanse.valanse.dto.Login.ReissueRequestDto;
import com.valanse.valanse.service.AuthService.AuthServiceImpl;
import com.valanse.valanse.service.KakaoService.KakaoServiceImpl;
import com.valanse.valanse.service.MemberService.MemberService;
import com.valanse.valanse.service.RefreshTokenService.RefreshTokenServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "1. 로그인 API", description = "카카오 로그인, 로그아웃, 토큰 재발급 관련 API")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RefreshTokenServiceImpl refreshTokenService;
    private final AuthServiceImpl authService;
    private final MemberService memberService;
    private final JwtTokenProvider jwtTokenProvider;
    private final KakaoServiceImpl kakaoService;

    @Value("${jwt.refresh-token-expiration}")
    private int refreshTokenExpirationMinutes;

    private long refreshTokenExpirationMillis;

    @PostConstruct
    public void init() {
        this.refreshTokenExpirationMillis = refreshTokenExpirationMinutes * 60L * 1000L;
    }

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
                    kakaoProfileDto.getKakao_account().getProfile().getProfile_image_url(),
                    accessTokenDto.getAccess_token(),
                    accessTokenDto.getRefresh_token()
            );
        }

        Map<String, String> jwtToken = jwtTokenProvider.createTokenPair(
                originalMember.getId(),
                originalMember.getRole().toString()
        );

        // jwt refresh token 저장
        refreshTokenService.saveRefreshToken(
                originalMember.getId().toString(),
                jwtToken.get("refreshToken"),
                refreshTokenExpirationMillis
        );

        Map<String, Object> loginInfo = new HashMap<>();
        loginInfo.put("id", originalMember.getId());
        loginInfo.put("accessToken", jwtToken.get("accessToken"));
        loginInfo.put("refreshToken", jwtToken.get("refreshToken"));

        return ResponseEntity.ok(loginInfo);
    }

    @Operation(summary = "로그아웃", description = "refresh token을 삭제하여 로그아웃 처리합니다. 이후부터는 기존의 refresh token으로 reissue를 시도하여도 새로운 access token 발급이 불가합니다.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        authService.logout();
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "access token 재발급",
            description = "만료된 access token 대신 refresh token을 사용해 새로운 access token을 발급합니다. "
                    + "refresh token이 유효하지 않거나 저장된 값과 다르면 401 에러를 반환합니다."
    )
    @PostMapping("/reissue")
    public ResponseEntity<Map<String, String>> reissueToken(@RequestBody ReissueRequestDto request) {
        String refreshToken = request.getRefreshToken();
        Map<String, String> tokenPair = authService.reissueAccessToken(refreshToken);
        return ResponseEntity.ok(tokenPair);
    }

    @Operation(
            summary = "회원 탈퇴 처리",
            description = "refresh token을 redis로부터 삭제해서 토큰 재발급 방지, 카카오 로그인 연결끊기, 그리고 사용자 데이터베이스 삭제를 진행합니다. "
    )
    @PostMapping("/withdraw")
    public ResponseEntity<Void> withdraw() {
        kakaoService.unLink(); // 카카오 로그인 연결 끊기
        authService.logout(); // refresh token을 redis로부터 삭제
        memberService.deleteMemberById(); // 사용자 데이터베이스 내용 삭제
        return ResponseEntity.ok().build();
    }
}
