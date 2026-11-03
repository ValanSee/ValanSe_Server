package com.valanse.valanse.dto.Login;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * Apple 로그인 요청 값을 담는 DTO 코드입니다.
 * iOS 앱(네이티브)은 redirectUri 없이, 웹(Apple JS)은 Apple 에 등록한 redirectUri 와 함께 보냅니다.
 */
public class AppleLoginRequest {
    private String identityToken;

    private String authorizationCode;

    private String redirectUri;

    // Apple 은 최초 동의 시에만 이름을 클라이언트로 전달하므로 선택값
    private String name;
}
