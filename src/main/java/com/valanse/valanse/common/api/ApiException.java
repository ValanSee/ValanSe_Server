package com.valanse.valanse.common.api;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;
//예상 되거나 제어되는 오류.
//이 프로젝트에서 발생하는 특정 예외 상황(예: 인증 실패, 데이터 없음)을 처리하기 위한 커스텀 예외 클래스입니다.
//RuntimeException을 상속받아 일반적인 오류와 구분하며,
//HTTP 상태 코드(HttpStatus)를 포함하여 API 응답에 적절한 상태를 반환할 수 있도록 합니다.
@Schema(hidden = true)
/**
 * 공통 API 예외와 응답 처리를 담당하는 코드입니다.
 */
public class ApiException extends RuntimeException {
    private final HttpStatus status;

    /**
     * ApiException 의존성을 주입하거나 객체를 초기화하는 생성자입니다.
     */
    public ApiException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    /**
     * Status 정보를 조회하는 메서드입니다.
     */
    public HttpStatus getStatus() {
        return status;
    }
}
