package com.valanse.valanse.controller;

import com.valanse.valanse.dto.MemberProfile.MemberPointRankingResponse;
import com.valanse.valanse.service.MemberProfileService.MemberProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "포인트 관련 API", description = "포인트 정보와 관련한 API 입니다.")
@RestController
@RequestMapping("/point")
@RequiredArgsConstructor
public class PointController {
    private final MemberProfileService memberProfileService;

    @Operation(summary = "포인트 랭킹표",
            description = "포인트 랭킹을 5위까지 반환합니다.")
    @GetMapping("/ranking")
    public ResponseEntity<MemberPointRankingResponse> getPointRanking() {
        return ResponseEntity.ok(memberProfileService.getPointRanking());
    }
}
