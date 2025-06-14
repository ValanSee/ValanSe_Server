package com.valanse.valanse.service.KakaoService;

import com.valanse.valanse.dto.Login.AccessTokenDto;
import com.valanse.valanse.dto.Login.KakaoProfileDto;

public interface KakaoService {

    AccessTokenDto getAccessToken(String code);
    KakaoProfileDto getKakaoProfile(String token);
    void unLink();
}
