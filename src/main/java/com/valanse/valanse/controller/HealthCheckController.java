package com.valanse.valanse.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "서버 확인 API", description = "서버가 실행 중인지를 테스트하는 용도입니다.")
@RestController
@RequestMapping("/health")
/**
 * 서버 상태 확인용 헬스 체크 컨트롤러 코드입니다.
 */
public class HealthCheckController {

    /**
     * HealthCheckController의 healthCheck 기능을 수행하는 메서드입니다.
     */
    @GetMapping
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Server is up and running");
    }
}
