package com.valanse.valanse.common.api;

import com.valanse.valanse.common.alert.ServerErrorEvent;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.time.Instant;
//예상치 못한 오류를 다룸.
@RestControllerAdvice
/**
 * 애플리케이션 전역 예외를 API 응답 형식으로 변환하는 공통 예외 처리 코드입니다.
 */
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private final ApplicationEventPublisher eventPublisher;

    @Value("${spring.profiles.active:}")
    private String activeProfiles;

    public GlobalExceptionHandler(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * GlobalExceptionHandler의 handleApiException 기능을 수행하는 메서드입니다.
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<?> handleApiException(ApiException e, HttpServletRequest request) {
        String traceId = null;
        if (e.getStatus().is5xxServerError()) {
            traceId = UUID.randomUUID().toString();
            log.error("Controlled API server exception. traceId={}", traceId, e);
            publishServerError(e, e.getStatus(), request, traceId);
        }

        Map<String, Object> error = new HashMap<>();
        error.put("error", e.getMessage());
        error.put("status", e.getStatus().value());
        if (traceId != null) {
            error.put("traceId", traceId);
        }
//        error.put("type", e.getClass().getSimpleName());

        ResponseEntity.BodyBuilder response = ResponseEntity.status(e.getStatus());
        if (traceId != null) {
            response.header(TRACE_ID_HEADER, traceId);
        }
        return response.body(error);
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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getDefaultMessage())
                .orElse("잘못된 요청입니다.");

        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        error.put("status", HttpStatus.BAD_REQUEST.value());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<?> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException e
    ) {
        return badRequest(e.getParameterName() + " 파라미터를 입력해주세요.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e
    ) {
        return badRequest(e.getName() + " 파라미터 형식이 올바르지 않습니다.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFoundException(
            NoResourceFoundException e
    ) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", "요청한 API를 찾을 수 없습니다.");
        error.put("status", HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * GlobalExceptionHandler의 handleUnexpectedException 기능을 수행하는 메서드입니다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleUnexpectedException(Exception e, HttpServletRequest request) {
        String traceId = UUID.randomUUID().toString();
        log.error("Unexpected API exception. traceId={}", traceId, e);
        publishServerError(e, HttpStatus.INTERNAL_SERVER_ERROR, request, traceId);

        Map<String, Object> error = new HashMap<>();
        error.put("error", "서버 내부 오류가 발생했습니다.");
        error.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        error.put("traceId", traceId);
        if (!isProdProfile()) {
            error.put("message", e.getMessage());
            error.put("type", e.getClass().getSimpleName());
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header(TRACE_ID_HEADER, traceId)
                .body(error);
    }

    private void publishServerError(
            Exception exception,
            HttpStatus status,
            HttpServletRequest request,
            String traceId
    ) {
        ServerErrorEvent event = new ServerErrorEvent(
                Instant.now(),
                activeProfiles == null || activeProfiles.isBlank() ? "default" : activeProfiles,
                status.value(),
                request.getMethod(),
                request.getRequestURI(),
                exception.getClass().getSimpleName(),
                exception.getMessage(),
                traceId,
                exception
        );

        try {
            eventPublisher.publishEvent(event);
        } catch (Exception publishException) {
            log.error("Failed to publish server error event. traceId={}", traceId, publishException);
        }
    }

    private boolean isProdProfile() {
        if (activeProfiles == null || activeProfiles.isBlank()) {
            return false;
        }

        return Arrays.stream(activeProfiles.split(","))
                .map(String::trim)
                .anyMatch("prod"::equals);
    }

    private ResponseEntity<Map<String, Object>> badRequest(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        error.put("status", HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
