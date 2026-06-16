package com.valanse.valanse.common.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;
//예상치 못한 오류를 다룸.
@RestControllerAdvice
/**
 * 애플리케이션 전역 예외를 API 응답 형식으로 변환하는 공통 예외 처리 코드입니다.
 * check: 운영 환경에서는 내부 예외 message/type을 응답에서 제거하고 로그 traceId로 추적해야 합니다.
 */
public class GlobalExceptionHandler {

    /**
     * GlobalExceptionHandler의 handleApiException 기능을 수행하는 메서드입니다.
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<?> handleApiException(ApiException e) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", e.getMessage());
        error.put("status", e.getStatus().value());
//        error.put("type", e.getClass().getSimpleName());

        return ResponseEntity.status(e.getStatus()).body(error);
    }

    // 예상치 못한 IllegalArgumentException을 400 에러로 포장해서 내보내기 위함
    /**
     * GlobalExceptionHandler의 handleIllegalArgumentException 기능을 수행하는 메서드입니다.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException e) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", e.getMessage());
        error.put("status", HttpStatus.BAD_REQUEST.value());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * GlobalExceptionHandler의 handleUnexpectedException 기능을 수행하는 메서드입니다.
     * check: 운영 응답에는 내부 예외 상세를 포함하지 않는 것이 안전합니다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleUnexpectedException(Exception e) {
        // TODO: message, type은 추후 배포 환경에서는 주석처리하기 (API 연동 목적으로 작성함)
        Map<String, Object> error = new HashMap<>();
        error.put("error", "서버 내부 오류가 발생했습니다.");
        error.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        error.put("message", e.getMessage());
        error.put("type", e.getClass().getSimpleName());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
