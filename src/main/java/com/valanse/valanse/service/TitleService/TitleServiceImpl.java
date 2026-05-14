package com.valanse.valanse.service.TitleService;

import com.valanse.valanse.domain.MemberProfile;
import com.valanse.valanse.domain.MemberProfileTitle;
import com.valanse.valanse.domain.Title;
import com.valanse.valanse.dto.Title.TitleEquipResponse;
import com.valanse.valanse.dto.Title.TitleListResponse;
import com.valanse.valanse.dto.Title.TitlePurchaseResponse;
import com.valanse.valanse.repository.MemberProfileRepository;
import com.valanse.valanse.repository.MemberProfileTitleRepository;
import com.valanse.valanse.repository.TitleRepository;
import lombok.RequiredArgsConstructor;
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

    private final MemberProfileRepository memberProfileRepository;
    private final MemberProfileTitleRepository memberProfileTitleRepository;
    private final TitleRepository titleRepository;

    @Override
    public TitleListResponse getTitleList(Long userId) {
        MemberProfile profile = memberProfileRepository.findByMemberId(userId)
                .orElseThrow(() -> new IllegalArgumentException("프로필이 존재하지 않습니다."));

        List<Title> titles = titleRepository.findAllByActiveTrueOrderByDisplayOrderAscIdAsc();
        List<MemberProfileTitle> profileTitles = memberProfileTitleRepository.findAllByMemberProfileMemberId(userId);
        Map<Long, MemberProfileTitle> ownedTitleMap = profileTitles.stream()
                .collect(Collectors.toMap(
                        profileTitle -> profileTitle.getTitle().getId(),
                        Function.identity(),
                        (current, ignored) -> current
                ));

        List<MemberProfileTitle> defaultTitlesToSave = titles.stream()
                .filter(Title::isDefaultTitle)
                .filter(title -> !ownedTitleMap.containsKey(title.getId()))
                .map(title -> MemberProfileTitle.builder()
                        .memberProfile(profile)
                        .title(title)
                        .build())
                .toList();

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
                .orElseThrow(() -> new IllegalArgumentException("프로필이 존재하지 않습니다."));

        MemberProfileTitle targetTitle = memberProfileTitleRepository
                .findByMemberProfileMemberIdAndTitleId(userId, titleId)
                .orElseThrow(() -> new IllegalArgumentException("보유하지 않은 칭호입니다."));

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
                .orElseThrow(() -> new IllegalArgumentException("프로필이 존재하지 않습니다."));

        Title title = titleRepository.findById(titleId)
                .orElseThrow(() -> new IllegalArgumentException("칭호가 존재하지 않습니다."));

        if (!title.isPointPurchaseTitle() || !title.isActive()) {
            throw new IllegalArgumentException("구매할 수 없는 칭호입니다.");
        }

        memberProfileTitleRepository.findByMemberProfileMemberIdAndTitleId(userId, titleId)
                .ifPresent(ownedTitle -> {
                    throw new IllegalArgumentException("이미 보유한 칭호입니다.");
                });

        if (!profile.hasEnoughPoint(title.getPrice())) {
            throw new IllegalArgumentException("포인트가 부족합니다. (필요포인트 " + title.getPrice() + "P 필요)");
        }

        profile.subtractPoint(title.getPrice());
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
}
