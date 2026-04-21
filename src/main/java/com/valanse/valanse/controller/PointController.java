package com.valanse.valanse.controller;

import com.valanse.valanse.dto.MemberProfile.MemberPointRankingResponse;
import com.valanse.valanse.service.MemberProfileService.MemberProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/point")
@RequiredArgsConstructor
public class PointController {

    private final MemberProfileService memberProfileService;

    @GetMapping("/ranking")
    public ResponseEntity<List<MemberPointRankingResponse>> getPointRanking() {
        return ResponseEntity.ok(memberProfileService.getPointRanking());
    }
}
