package com.valanse.valanse.service.KakaoService;

import com.valanse.valanse.dto.Login.AccessTokenDto;
import com.valanse.valanse.dto.Login.KakaoProfileDto;

/**
 * KakaoService 기능의 비즈니스 계약을 정의하는 서비스 인터페이스 코드입니다.
 */
public interface KakaoService {

    AccessTokenDto getAccessToken(String code);
    KakaoProfileDto getKakaoProfile(String token);
    void unLink();
}
