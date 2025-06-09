package com.valanse.valanse.controller;

import com.valanse.valanse.dto.MemberProfile.MemberMyPageResponse;
import com.valanse.valanse.dto.MemberProfile.MemberProfileRequest;
import com.valanse.valanse.dto.MemberProfile.MemberProfileResponse;
import com.valanse.valanse.service.MemberProfileService.MemberProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "2. 회원 API", description = "프로필 조회 등 회원 정보 관련 기능")
@RestController
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberProfileService memberProfileService;

    @Operation(
            summary = "회원 프로필 정보 저장",
            description = "닉네임, 성별, 나이, MBTI 정보를 저장하거나 수정합니다. 모든 필드가 채워진 경우에만 저장됩니다. 만약 아직 프로필 정보가 없는 경우 null 을 반환하니 식별에 사용하시면 됩니다."
    )
    @PostMapping("/profile")
    public ResponseEntity<Void> saveProfile(@RequestBody MemberProfileRequest dto) {
        memberProfileService.saveOrUpdateProfile(dto);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "회원 프로필 추가 정보 조회",
            description = "현재 로그인한 회원의 추가 프로필 정보를 조회합니다. 정보가 없으면 'profile: null' 형태로 반환됩니다."
    )
    @GetMapping("/profile")
    public ResponseEntity<MemberProfileResponse> getProfile() {
        MemberProfileResponse response = memberProfileService.getProfile();
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "마이페이지 회원 프로필 정보 조회",
            description = "현재 로그인한 회원의 프로필 정보를 조회합니다. 정보가 없으면 'profile: null' 형태로 반환합니다."
    )
    @GetMapping("/mypage")
    public ResponseEntity<MemberMyPageResponse> getMyProfile() {
        MemberMyPageResponse response = memberProfileService.getMyProfile();
        return ResponseEntity.ok(response);
    }
}
