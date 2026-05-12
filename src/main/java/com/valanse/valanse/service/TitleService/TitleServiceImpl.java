package com.valanse.valanse.service.TitleService;

import com.valanse.valanse.domain.MemberProfileTitle;
import com.valanse.valanse.dto.Title.TitleEquipResponse;
import com.valanse.valanse.repository.MemberProfileRepository;
import com.valanse.valanse.repository.MemberProfileTitleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TitleServiceImpl implements TitleService {

    private final MemberProfileRepository memberProfileRepository;
    private final MemberProfileTitleRepository memberProfileTitleRepository;

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
}
