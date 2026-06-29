package com.valanse.valanse.dto.Login;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * RedirectDto API 응답 또는 계층 간 전달 값을 담는 DTO 코드입니다.
 */
public class RedirectDto {
    private String code;

    @JsonAlias("redirect_uri")
    private String redirectUri;
}
