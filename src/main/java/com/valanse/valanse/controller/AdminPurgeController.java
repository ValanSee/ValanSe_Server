package com.valanse.valanse.controller;

import com.valanse.valanse.service.PurgeService.PurgePreview;
import com.valanse.valanse.service.PurgeService.SoftDeletePurgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Tag(name = "관리자 purge API", description = "관리자 전용 삭제 대상 확인 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/purge")
public class AdminPurgeController {
    private final SoftDeletePurgeService purgeService;

    @GetMapping("/preview")
    @Operation(summary = "Purge dry-run", description = "실제 삭제 없이 14일이 지난 댓글, 투표, 회원 건수를 조회합니다.")
    public ResponseEntity<PurgePreview> preview() {
        return ResponseEntity.ok(purgeService.preview(LocalDateTime.now()));
    }
}
