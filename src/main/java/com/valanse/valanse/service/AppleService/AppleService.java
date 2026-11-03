package com.valanse.valanse.service.AppleService;

import com.valanse.valanse.domain.Member;
import com.valanse.valanse.dto.Login.AppleIdentity;

/**
 * AppleService 기능의 비즈니스 계약을 정의하는 서비스 인터페이스 코드입니다.
 */
public interface AppleService {

    AppleIdentity verifyIdentityToken(String identityToken);

    /**
     * authorization code를 Apple refresh token으로 교환하고, 교환 응답의 id_token을
     * expectedIdentity(최초 identity token 검증 결과)와 대조해 같은 사용자인지 확인한다.
     */
    String issueRefreshToken(String authorizationCode, AppleIdentity expectedIdentity, String redirectUri);

    void revoke(Member member);

    /**
     * 회원 엔티티 없이, 발급받은 refresh token과 clientId만으로 revoke한다.
     * (회원 저장 실패 시 일회용으로 발급된 refresh token을 보상 revoke하기 위한 용도)
     */
    void revoke(String refreshToken, String clientId);
}
