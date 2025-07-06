package com.valanse.valanse.common.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;
//예상치 못한 오류를 다룸.
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<?> handleApiException(ApiException e) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", e.getMessage());
        error.put("status", e.getStatus().value());
        error.put("type", e.getClass().getSimpleName());

        return ResponseEntity.status(e.getStatus()).body(error);
    }


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

