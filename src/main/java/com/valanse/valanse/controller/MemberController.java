package com.valanse.valanse.controller;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.common.message.MemberErrorMessage;
import com.valanse.valanse.dto.MemberProfile.MemberMyPageResponse;
import com.valanse.valanse.dto.MemberProfile.MemberProfileRequest;
import com.valanse.valanse.dto.MemberProfile.MemberProfileResponse;
import com.valanse.valanse.dto.PointHistory.PointHistoryResponse;
import com.valanse.valanse.dto.Title.TitleCreateRequest;
import com.valanse.valanse.dto.Title.TitleCreateResponse;
import com.valanse.valanse.dto.Title.TitleAdminResponse;
import com.valanse.valanse.dto.Title.TitleDeleteResponse;
import com.valanse.valanse.dto.Title.TitleEquipResponse;
import com.valanse.valanse.dto.Title.TitleListResponse;
import com.valanse.valanse.dto.Title.TitlePurchaseResponse;
import com.valanse.valanse.dto.Title.TitleUpdateRequest;
import com.valanse.valanse.dto.Title.TitleUpdateResponse;
import com.valanse.valanse.service.MemberProfileService.MemberProfileService;
import com.valanse.valanse.service.PointService.PointService;
import com.valanse.valanse.service.TitleService.TitleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "회원 정보 API", description = "프로필 조회 등 회원 정보 관련 기능")
@RestController
@RequestMapping("/member")
@RequiredArgsConstructor
/**
 * 회원 프로필, 마이페이지, 포인트, 칭호 관련 요청을 처리하는 컨트롤러 코드입니다.
 */
public class MemberController {

    private final MemberProfileService memberProfileService;
    private final PointService pointService;
    private final TitleService titleService;

    @Operation(
            summary = "회원 프로필 정보 저장",
            description = "닉네임, 성별, 나이, MBTI 정보를 저장하거나 수정합니다. 모든 필드가 채워진 경우에만 저장됩니다. " +
                    "MBTI는 IE와 TF를 모두 선택해야 하며, 최종 MBTI는 4자리여야 합니다 (예: ENFP)."
    )
    /**
     * Profile 데이터를 저장하는 메서드입니다.
     */
    @PostMapping("/profile")
    public ResponseEntity<Void> saveProfile(@RequestBody MemberProfileRequest dto) {
        // ✅ 추가: 입력값 기본 검증
        if (dto.nickname() == null || dto.nickname().trim().isEmpty()) {
            throw new ApiException(MemberErrorMessage.NICKNAME_REQUIRED.message(), HttpStatus.BAD_REQUEST);
        }

        if (dto.gender() == null) {
            throw new ApiException(MemberErrorMessage.GENDER_REQUIRED.message(), HttpStatus.BAD_REQUEST);
        }

        if (dto.age() == null) {
            throw new ApiException(MemberErrorMessage.AGE_REQUIRED.message(), HttpStatus.BAD_REQUEST);
        }

        // MBTI 검증 (Service에서도 한 번 더 검증)
        if (dto.mbtiIe() == null || dto.mbtiTf() == null) {
            throw new ApiException(MemberErrorMessage.MBTI_REQUIRED.message(), HttpStatus.BAD_REQUEST);
        }

        if (dto.mbti() == null || dto.mbti().length() != 4) {
            throw new ApiException(MemberErrorMessage.MBTI_INVALID_LENGTH.message(), HttpStatus.BAD_REQUEST);
        }

        memberProfileService.saveOrUpdateProfile(dto);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "회원 프로필 추가 정보 조회",
            description = "현재 로그인한 회원의 추가 프로필 정보를 조회합니다. 정보가 없으면 'profile: null' 형태로 반환됩니다."
    )
    /**
     * Profile 정보를 조회하는 메서드입니다.
     */
    @GetMapping("/profile")
    public ResponseEntity<MemberProfileResponse> getProfile() {
        MemberProfileResponse response = memberProfileService.getProfile();
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "회원 닉네임 유효성 검사",
            description = "추가 정보 입력 단계에서 닉네임의 유효성을 검사합니다. 각 항목별로 boolean 형태로 값을 반환합니다."
    )
    /**
     * MemberController의 checkNicknameDuplicate 기능을 수행하는 메서드입니다.
     */
    @GetMapping("/check-nickname")
    public ResponseEntity<Map<String, Boolean>> checkNicknameDuplicate(@RequestParam(name = "nickname")  String nickname) {
        boolean isAvailable = memberProfileService.isAvailableNickname(nickname);
        boolean isMeaningful = memberProfileService.isMeaningfulNickname(nickname);
        boolean isClean = memberProfileService.isCleanNickname(nickname);

        Map<String, Boolean> response = new HashMap<>();
        // true = 사용 가능한 닉네임 (긍정) 으로 통일!
        response.put("isAvailable", isAvailable);
        response.put("isMeaningful", isMeaningful);
        response.put("isClean", isClean);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "마이페이지 회원 프로필 정보 조회",
            description = "현재 로그인한 회원의 프로필 정보를 조회합니다. 정보가 없으면 'profile: null' 형태로 반환합니다."
    )
    /**
     * MyProfile 정보를 조회하는 메서드입니다.
     */
    @GetMapping("/mypage")
    public ResponseEntity<MemberMyPageResponse> getMyProfile() {
        MemberMyPageResponse response = memberProfileService.getMyProfile();
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "포인트 지급 내역 조회",
            description = "현재 로그인한 회원의 포인트 지급 내역을 조회합니다. 최신순으로 정렬되어 반환됩니다."
    )
    /**
     * 회원의 포인트 변동 이력을 최신순 응답으로 조회하는 메서드입니다.
     */
    @GetMapping("/point-history")
    public ResponseEntity<PointHistoryResponse> getPointHistory() {
        Long userId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
        PointHistoryResponse response = pointService.getPointHistory(userId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "칭호 선택 목록 조회",
            description = "현재 로그인한 회원 기준으로 기본, 보유, 미보유 칭호를 분리해서 조회합니다."
    )
    /**
     * Titles 정보를 조회하는 메서드입니다.
     */
    @GetMapping("/titles")
    public ResponseEntity<TitleListResponse> getTitles() {
        Long userId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
        TitleListResponse response = titleService.getTitleList(userId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "관리자 칭호 목록 조회",
            description = "관리자 권한으로 잠김/보유 여부와 상관없이 칭호 마스터 데이터 목록을 조회합니다."
    )
    /**
     * TitlesForAdmin 정보를 조회하는 메서드입니다.
     */
    @GetMapping("/titles/admin")
    public ResponseEntity<List<TitleAdminResponse>> getTitlesForAdmin() {
        Long userId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
        List<TitleAdminResponse> response = titleService.getTitleListForAdmin(userId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "관리자 칭호 생성",
            description = "관리자 권한으로 새로운 칭호 마스터 데이터를 생성합니다."
    )
    /**
     * 관리자가 새 칭호 마스터 데이터를 생성하는 메서드입니다.
     */
    @PostMapping("/titles")
    public ResponseEntity<TitleCreateResponse> createTitle(@RequestBody TitleCreateRequest request) {
        Long userId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
        TitleCreateResponse response = titleService.createTitle(userId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "관리자 칭호 수정",
            description = "관리자 권한으로 칭호 마스터 데이터를 수정합니다."
    )
    /**
     * 관리자가 기존 칭호 마스터 데이터를 수정하는 메서드입니다.
     */
    @PatchMapping("/titles/{titleId}")
    public ResponseEntity<TitleUpdateResponse> updateTitle(
            @PathVariable Long titleId,
            @RequestBody TitleUpdateRequest request
    ) {
        Long userId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
        TitleUpdateResponse response = titleService.updateTitle(userId, titleId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "관리자 칭호 삭제",
            description = "관리자 권한으로 칭호를 비활성화합니다. 삭제 대상 칭호를 장착 중인 회원은 활성 기본 칭호로 변경됩니다."
    )
    /**
     * 관리자가 칭호를 비활성화하고 장착 회원을 기본 칭호로 이동시키는 메서드입니다.
     */
    @DeleteMapping("/titles/{titleId}")
    public ResponseEntity<TitleDeleteResponse> deleteTitle(@PathVariable Long titleId) {
        Long userId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
        TitleDeleteResponse response = titleService.deleteTitle(userId, titleId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "칭호 장착",
            description = "현재 로그인한 회원이 보유한 칭호를 대표 칭호로 선택합니다."
    )
    /**
     * 회원이 보유한 칭호를 장착하고 기존 장착 칭호를 해제하는 메서드입니다.
     */
    @PostMapping("/titles/{titleId}/equip")
    public ResponseEntity<TitleEquipResponse> equipTitle(@PathVariable Long titleId) {
        Long userId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
        TitleEquipResponse response = titleService.equipTitle(userId, titleId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "칭호 구매",
            description = "현재 로그인한 회원이 포인트로 구매 가능한 칭호를 구매합니다."
    )
    /**
     * 포인트 구매형 칭호를 구매하고 포인트를 차감하는 메서드입니다.
     */
    @PostMapping("/titles/{titleId}/purchase")
    public ResponseEntity<TitlePurchaseResponse> purchaseTitle(@PathVariable Long titleId) {
        Long userId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
        TitlePurchaseResponse response = titleService.purchaseTitle(userId, titleId);
        return ResponseEntity.ok(response);
    }
}
