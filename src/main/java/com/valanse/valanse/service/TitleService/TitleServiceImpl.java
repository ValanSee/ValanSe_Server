package com.valanse.valanse.service.TitleService;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.common.message.AuthErrorMessage;
import com.valanse.valanse.common.message.MemberErrorMessage;
import com.valanse.valanse.common.message.ProfileErrorMessage;
import com.valanse.valanse.common.message.TitleErrorMessage;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.MemberProfile;
import com.valanse.valanse.domain.MemberProfileTitle;
import com.valanse.valanse.domain.Title;
import com.valanse.valanse.domain.enums.Role;
import com.valanse.valanse.domain.enums.TitleAcquisitionType;
import com.valanse.valanse.domain.enums.PointType;
import com.valanse.valanse.dto.Title.TitleCreateRequest;
import com.valanse.valanse.dto.Title.TitleCreateResponse;
import com.valanse.valanse.dto.Title.TitleAdminResponse;
import com.valanse.valanse.dto.Title.TitleDeleteResponse;
import com.valanse.valanse.dto.Title.TitleEquipResponse;
import com.valanse.valanse.dto.Title.TitleListResponse;
import com.valanse.valanse.dto.Title.TitlePurchaseResponse;
import com.valanse.valanse.dto.Title.TitleUpdateRequest;
import com.valanse.valanse.dto.Title.TitleUpdateResponse;
import com.valanse.valanse.repository.MemberRepository;
import com.valanse.valanse.repository.MemberProfileRepository;
import com.valanse.valanse.repository.MemberProfileTitleRepository;
import com.valanse.valanse.repository.TitleRepository;
import com.valanse.valanse.service.PointService.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TitleServiceImpl implements TitleService {

    private final MemberRepository memberRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final MemberProfileTitleRepository memberProfileTitleRepository;
    private final TitleRepository titleRepository;
    private final PointService pointService;

    @Override
    public TitleListResponse getTitleList(Long userId) {
        MemberProfile profile = memberProfileRepository.findByMemberId(userId)
                .orElseThrow(() -> new ApiException(ProfileErrorMessage.PROFILE_NOT_FOUND.message(), HttpStatus.NOT_FOUND));

        List<Title> titles = titleRepository.findAllByActiveTrueOrderByDisplayOrderAscIdAsc();
        List<MemberProfileTitle> profileTitles = memberProfileTitleRepository.findAllByMemberProfileMemberId(userId);
        boolean hasEquippedTitle = profileTitles.stream().anyMatch(MemberProfileTitle::isEquipped);
        Map<Long, MemberProfileTitle> ownedTitleMap = profileTitles.stream()
                .collect(Collectors.toMap(
                        profileTitle -> profileTitle.getTitle().getId(),
                        Function.identity(),
                        (current, ignored) -> current
                ));

        List<MemberProfileTitle> defaultTitlesToSave = new ArrayList<>();
        for (Title title : titles) {
            if (!title.isDefaultTitle() || ownedTitleMap.containsKey(title.getId())) {
                continue;
            }

            MemberProfileTitle profileTitle = MemberProfileTitle.builder()
                    .memberProfile(profile)
                    .title(title)
                    .equipped(!hasEquippedTitle)
                    .build();
            if (!hasEquippedTitle) {
                hasEquippedTitle = true;
            }
            defaultTitlesToSave.add(profileTitle);
        }

        if (!defaultTitlesToSave.isEmpty()) {
            memberProfileTitleRepository.saveAll(defaultTitlesToSave)
                    .forEach(profileTitle -> ownedTitleMap.put(profileTitle.getTitle().getId(), profileTitle));
        }

        List<TitleListResponse.TitleSummaryResponse> defaultTitles = new ArrayList<>();
        List<TitleListResponse.TitleSummaryResponse> ownedTitles = new ArrayList<>();
        List<TitleListResponse.TitleSummaryResponse> lockedTitles = new ArrayList<>();

        for (Title title : titles) {
            MemberProfileTitle profileTitle = ownedTitleMap.get(title.getId());
            boolean owned = profileTitle != null || title.isDefaultTitle();
            boolean locked = !owned;

            TitleListResponse.TitleSummaryResponse response = toTitleSummary(title, profileTitle, owned, locked);
            if (title.isDefaultTitle()) {
                defaultTitles.add(response);
            } else if (owned) {
                ownedTitles.add(response);
            } else {
                lockedTitles.add(response);
            }
        }

        return new TitleListResponse(defaultTitles, ownedTitles, lockedTitles);
    }

    @Override
    public TitleEquipResponse equipTitle(Long userId, Long titleId) {
        memberProfileRepository.findByMemberId(userId)
                .orElseThrow(() -> new ApiException(ProfileErrorMessage.PROFILE_NOT_FOUND.message(), HttpStatus.NOT_FOUND));

        MemberProfileTitle targetTitle = memberProfileTitleRepository
                .findByMemberProfileMemberIdAndTitleId(userId, titleId)
                .orElseThrow(() -> new ApiException(TitleErrorMessage.UNOWNED_TITLE.message(), HttpStatus.BAD_REQUEST));

        if (!targetTitle.getTitle().isActive()) {
            throw new ApiException(TitleErrorMessage.TITLE_NOT_EQUIPPABLE.message(), HttpStatus.BAD_REQUEST);
        }

        memberProfileTitleRepository.findAllByMemberProfileMemberIdAndEquippedTrue(userId)
                .forEach(MemberProfileTitle::unequip);
        targetTitle.equip();

        return new TitleEquipResponse(
                targetTitle.getTitle().getId(),
                targetTitle.getTitle().getName(),
                targetTitle.isEquipped()
        );
    }

    @Override
    public TitlePurchaseResponse purchaseTitle(Long userId, Long titleId) {
        MemberProfile profile = memberProfileRepository.findByMemberId(userId)
                .orElseThrow(() -> new ApiException(ProfileErrorMessage.PROFILE_NOT_FOUND.message(), HttpStatus.NOT_FOUND));

        Title title = titleRepository.findById(titleId)
                .orElseThrow(() -> new ApiException(TitleErrorMessage.TITLE_NOT_FOUND.message(), HttpStatus.NOT_FOUND));

        if (!title.isPointPurchaseTitle() || !title.isActive()) {
            throw new ApiException(TitleErrorMessage.TITLE_NOT_PURCHASABLE.message(), HttpStatus.BAD_REQUEST);
        }

        memberProfileTitleRepository.findByMemberProfileMemberIdAndTitleId(userId, titleId)
                .ifPresent(ownedTitle -> {
                    throw new ApiException(TitleErrorMessage.TITLE_ALREADY_OWNED.message(), HttpStatus.BAD_REQUEST);
                });

        if (!profile.hasEnoughPoint(title.getPrice())) {
            throw new ApiException(TitleErrorMessage.POINT_NOT_ENOUGH.message(title.getPrice()), HttpStatus.BAD_REQUEST);
        }

        profile.subtractPoint(title.getPrice());
        pointService.recordPointUsage(userId, title.getPrice(), PointType.TITLE_PURCHASE);
        memberProfileTitleRepository.save(MemberProfileTitle.builder()
                .memberProfile(profile)
                .title(title)
                .build());

        return new TitlePurchaseResponse(
                title.getId(),
                title.getName(),
                true,
                profile.getPoint()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<TitleAdminResponse> getTitleListForAdmin(Long adminUserId) {
        validateAdmin(adminUserId);

        return titleRepository.findAllByOrderByDisplayOrderAscIdAsc().stream()
                .map(this::toTitleAdminResponse)
                .toList();
    }

    @Override
    public TitleCreateResponse createTitle(Long adminUserId, TitleCreateRequest request) {
        validateAdmin(adminUserId);

        validateCreateRequest(request);

        String code = request.code().trim();
        if (titleRepository.existsByCode(code)) {
            throw new ApiException(TitleErrorMessage.TITLE_CODE_DUPLICATED.message(), HttpStatus.BAD_REQUEST);
        }

        Title title = Title.builder()
                .code(code)
                .name(request.title().trim())
                .description(trimToNull(request.description()))
                .price(request.price() == null ? 0L : request.price())
                .tier(request.tier())
                .acquisitionType(request.acquisitionType())
                .requirementText(trimToNull(request.requirementText()))
                .active(request.active() == null || request.active())
                .displayOrder(request.displayOrder() == null ? 0 : request.displayOrder())
                .build();

        Title savedTitle = titleRepository.save(title);
        return toTitleCreateResponse(savedTitle);
    }

    @Override
    public TitleUpdateResponse updateTitle(Long adminUserId, Long titleId, TitleUpdateRequest request) {
        validateAdmin(adminUserId);

        validateUpdateRequest(request);

        Title title = titleRepository.findById(titleId)
                .orElseThrow(() -> new ApiException(TitleErrorMessage.TITLE_NOT_FOUND.message(), HttpStatus.NOT_FOUND));

        String code = request.code().trim();
        if (titleRepository.existsByCodeAndIdNot(code, titleId)) {
            throw new ApiException(TitleErrorMessage.TITLE_CODE_DUPLICATED.message(), HttpStatus.BAD_REQUEST);
        }

        title.update(
                code,
                request.title().trim(),
                trimToNull(request.description()),
                request.price() == null ? 0L : request.price(),
                request.tier(),
                request.acquisitionType(),
                trimToNull(request.requirementText()),
                request.active() == null || request.active(),
                request.displayOrder() == null ? 0 : request.displayOrder()
        );

        return toTitleUpdateResponse(title);
    }

    @Override
    public TitleDeleteResponse deleteTitle(Long adminUserId, Long titleId) {
        validateAdmin(adminUserId);

        Title title = titleRepository.findById(titleId)
                .orElseThrow(() -> new ApiException(TitleErrorMessage.TITLE_NOT_FOUND.message(), HttpStatus.NOT_FOUND));

        if (!title.isActive()) {
            throw new ApiException(TitleErrorMessage.TITLE_ALREADY_DELETED.message(), HttpStatus.BAD_REQUEST);
        }
        if (title.isDefaultTitle()) {
            throw new ApiException(TitleErrorMessage.DEFAULT_TITLE_DELETE_NOT_ALLOWED.message(), HttpStatus.BAD_REQUEST);
        }

        Title fallbackTitle = titleRepository
                .findFirstByActiveTrueAndAcquisitionTypeOrderByDisplayOrderAscIdAsc(TitleAcquisitionType.DEFAULT)
                .orElseThrow(() -> new ApiException(TitleErrorMessage.DEFAULT_TITLE_NOT_FOUND.message(), HttpStatus.INTERNAL_SERVER_ERROR));

        List<MemberProfileTitle> equippedTitles = memberProfileTitleRepository.findAllByTitleIdAndEquippedTrue(titleId);
        for (MemberProfileTitle equippedTitle : equippedTitles) {
            equippedTitle.unequip();
            MemberProfile profile = equippedTitle.getMemberProfile();
            MemberProfileTitle fallbackProfileTitle = memberProfileTitleRepository
                    .findByMemberProfileIdAndTitleId(profile.getId(), fallbackTitle.getId())
                    .orElseGet(() -> memberProfileTitleRepository.save(MemberProfileTitle.builder()
                            .memberProfile(profile)
                            .title(fallbackTitle)
                            .build()));
            fallbackProfileTitle.equip();
        }
        title.deactivate();

        return new TitleDeleteResponse(
                title.getId(),
                title.getName(),
                fallbackTitle.getId(),
                fallbackTitle.getName(),
                equippedTitles.size(),
                title.isActive()
        );
    }

    private void validateAdmin(Long adminUserId) {
        Member admin = memberRepository.findByIdAndDeletedAtIsNull(adminUserId)
                .orElseThrow(() -> new ApiException(MemberErrorMessage.MEMBER_NOT_FOUND.message(), HttpStatus.NOT_FOUND));
        if (admin.getRole() != Role.ADMIN) {
            throw new ApiException(AuthErrorMessage.ADMIN_ONLY.message(), HttpStatus.FORBIDDEN);
        }
    }

    private TitleListResponse.TitleSummaryResponse toTitleSummary(
            Title title,
            MemberProfileTitle profileTitle,
            boolean owned,
            boolean locked
    ) {
        return new TitleListResponse.TitleSummaryResponse(
                title.getId(),
                title.getName(),
                title.getDescription(),
                title.getTier(),
                title.getAcquisitionType(),
                owned,
                profileTitle != null && profileTitle.isEquipped(),
                locked,
                title.getPrice(),
                title.getRequirementText(),
                locked ? getLockReason(title) : null
        );
    }

    private String getLockReason(Title title) {
        if (title.getRequirementText() != null && !title.getRequirementText().isBlank()) {
            return title.getRequirementText();
        }
        if (title.isPointPurchaseTitle()) {
            return title.getPrice() + "P 필요";
        }
        return "획득 조건을 달성해야 합니다.";
    }

    private TitleAdminResponse toTitleAdminResponse(Title title) {
        return new TitleAdminResponse(
                title.getId(),
                title.getCode(),
                title.getName(),
                title.getDescription(),
                title.getPrice(),
                title.getTier(),
                title.getAcquisitionType(),
                title.getRequirementText(),
                title.getDisplayOrder()
        );
    }

    private void validateCreateRequest(TitleCreateRequest request) {
        if (request == null) {
            throw new ApiException(TitleErrorMessage.CREATE_REQUEST_EMPTY.message(), HttpStatus.BAD_REQUEST);
        }
        if (isBlank(request.code())) {
            throw new ApiException(TitleErrorMessage.CODE_REQUIRED.message(), HttpStatus.BAD_REQUEST);
        }
        if (isBlank(request.title())) {
            throw new ApiException(TitleErrorMessage.NAME_REQUIRED.message(), HttpStatus.BAD_REQUEST);
        }
        if (request.tier() == null) {
            throw new ApiException(TitleErrorMessage.TIER_REQUIRED.message(), HttpStatus.BAD_REQUEST);
        }
        if (request.acquisitionType() == null) {
            throw new ApiException(TitleErrorMessage.ACQUISITION_TYPE_REQUIRED.message(), HttpStatus.BAD_REQUEST);
        }
        if (request.price() != null && request.price() < 0) {
            throw new ApiException(TitleErrorMessage.PRICE_INVALID.message(), HttpStatus.BAD_REQUEST);
        }
        if (request.displayOrder() != null && request.displayOrder() < 0) {
            throw new ApiException(TitleErrorMessage.DISPLAY_ORDER_INVALID.message(), HttpStatus.BAD_REQUEST);
        }
    }

    private void validateUpdateRequest(TitleUpdateRequest request) {
        if (request == null) {
            throw new ApiException(TitleErrorMessage.UPDATE_REQUEST_EMPTY.message(), HttpStatus.BAD_REQUEST);
        }
        if (isBlank(request.code())) {
            throw new ApiException(TitleErrorMessage.CODE_REQUIRED.message(), HttpStatus.BAD_REQUEST);
        }
        if (isBlank(request.title())) {
            throw new ApiException(TitleErrorMessage.NAME_REQUIRED.message(), HttpStatus.BAD_REQUEST);
        }
        if (request.tier() == null) {
            throw new ApiException(TitleErrorMessage.TIER_REQUIRED.message(), HttpStatus.BAD_REQUEST);
        }
        if (request.acquisitionType() == null) {
            throw new ApiException(TitleErrorMessage.ACQUISITION_TYPE_REQUIRED.message(), HttpStatus.BAD_REQUEST);
        }
        if (request.price() != null && request.price() < 0) {
            throw new ApiException(TitleErrorMessage.PRICE_INVALID.message(), HttpStatus.BAD_REQUEST);
        }
        if (request.displayOrder() != null && request.displayOrder() < 0) {
            throw new ApiException(TitleErrorMessage.DISPLAY_ORDER_INVALID.message(), HttpStatus.BAD_REQUEST);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private TitleCreateResponse toTitleCreateResponse(Title title) {
        return new TitleCreateResponse(
                title.getId(),
                title.getCode(),
                title.getName(),
                title.getDescription(),
                title.getPrice(),
                title.getTier(),
                title.getAcquisitionType(),
                title.getRequirementText(),
                title.isActive(),
                title.getDisplayOrder()
        );
    }

    private TitleUpdateResponse toTitleUpdateResponse(Title title) {
        return new TitleUpdateResponse(
                title.getId(),
                title.getCode(),
                title.getName(),
                title.getDescription(),
                title.getPrice(),
                title.getTier(),
                title.getAcquisitionType(),
                title.getRequirementText(),
                title.isActive(),
                title.getDisplayOrder()
        );
    }
}
