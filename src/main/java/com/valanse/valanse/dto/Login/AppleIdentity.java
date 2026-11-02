package com.valanse.valanse.dto.Login;

/**
 * 검증된 Apple identity token 에서 꺼낸 사용자 정보입니다.
 *
 * @param userId   Apple 사용자 고유 ID (sub)
 * @param email    이메일 (이메일 가리기 선택 시 privaterelay 주소, 없을 수 있음)
 * @param clientId 토큰을 발급받은 client_id (aud, 앱 번들 ID 또는 웹 Services ID)
 */
public record AppleIdentity(String userId, String email, String clientId) {
}
