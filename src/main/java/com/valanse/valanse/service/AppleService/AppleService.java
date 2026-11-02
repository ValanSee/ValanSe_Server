package com.valanse.valanse.service.AppleService;

import com.valanse.valanse.domain.Member;
import com.valanse.valanse.dto.Login.AppleIdentity;

/**
 * AppleService 기능의 비즈니스 계약을 정의하는 서비스 인터페이스 코드입니다.
 */
public interface AppleService {

    AppleIdentity verifyIdentityToken(String identityToken);
    String issueRefreshToken(String authorizationCode, String clientId, String redirectUri);
    void revoke(Member member);
}
