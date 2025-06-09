package com.valanse.valanse.service;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.dto.Login.AccessTokenDto;
import com.valanse.valanse.dto.Login.KakaoProfileDto;
import com.valanse.valanse.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;


@Service
public class KakaoService {
    private final MemberRepository memberRepository;
    @Value("${oauth.kakao.client-id}")
    private String kakaoClientId;

    @Value("${oauth.kakao.redirect-uri}")
    private String kakaoRedirectUri;

    public KakaoService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public AccessTokenDto getAccessToken(String code) {
        RestClient restClient = RestClient.create();

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", kakaoClientId);
        params.add("redirect_uri", kakaoRedirectUri);
        params.add("grant_type", "authorization_code");

        ResponseEntity<AccessTokenDto> response = restClient.post()
                .uri("https://kauth.kakao.com/oauth/token")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body(params)
                .retrieve()
                .toEntity(AccessTokenDto.class);

//        System.out.println("access token: " + response.getBody().getAccess_token());

        return response.getBody();
    }

    public KakaoProfileDto getKakaoProfile(String token) {
        RestClient restClient = RestClient.create();

        ResponseEntity<KakaoProfileDto> response = restClient.get()
                .uri("https://kapi.kakao.com/v2/user/me") // 고정된 값임. 항상 이렇게 입력하기
                .header("Authorization", "Bearer " + token)
                .retrieve() // 응답의 body 값만을 추출하는 메소드
                .toEntity(KakaoProfileDto.class);

        System.out.println("response.getBody() = " + response.getBody());
        return response.getBody();
    }

    public void unLink() {
        Long userId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
        Member member = memberRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException("회원 정보가 존재하지 않습니다.", HttpStatus.NOT_FOUND));

        String refreshToken = member.getKakaoRefreshToken();
        if (refreshToken == null) {
            throw new ApiException("카카오 RefreshToken이 존재하지 않습니다.", HttpStatus.BAD_REQUEST);
        }

        // 카카오 서버 측에 요청 보내기
        RestClient restClient = RestClient.create();

        // 1. access token 재발급 요청
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("refresh_token", refreshToken);
        params.add("client_id", kakaoClientId);
        params.add("grant_type", "refresh_token");

        AccessTokenDto tokenDto = restClient.post()
                .uri("https://kauth.kakao.com/oauth/token")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body(params)
                .retrieve()
                .body(AccessTokenDto.class);

        if (tokenDto == null || tokenDto.getAccess_token() == null) {
            throw new ApiException("카카오 access token 재발급 실패", HttpStatus.UNAUTHORIZED);
        }

        // 2. unlink 요청 (access token 사용)
        restClient.post()
                .uri("https://kapi.kakao.com/v1/user/unlink")
                .header("Authorization", "Bearer " + tokenDto.getAccess_token())
                .retrieve()
                .toBodilessEntity();  // 응답 바디 없음
    }

}
