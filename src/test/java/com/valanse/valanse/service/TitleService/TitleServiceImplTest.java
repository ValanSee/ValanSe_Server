package com.valanse.valanse.service.TitleService;

import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.MemberProfile;
import com.valanse.valanse.domain.MemberProfileTitle;
import com.valanse.valanse.domain.Title;
import com.valanse.valanse.domain.enums.TitleAcquisitionType;
import com.valanse.valanse.domain.enums.TitleTier;
import com.valanse.valanse.repository.MemberProfileRepository;
import com.valanse.valanse.repository.MemberProfileTitleRepository;
import com.valanse.valanse.repository.TitleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TitleServiceImplTest {

    @InjectMocks
    private TitleServiceImpl titleService;

    @Mock
    private MemberProfileRepository memberProfileRepository;

    @Mock
    private MemberProfileTitleRepository memberProfileTitleRepository;

    @Mock
    private TitleRepository titleRepository;

    private Member member;
    private MemberProfile profile;

    @BeforeEach
    void setup() {
        member = Member.builder()
                .id(1L)
                .email("test@email.com")
                .nickname("테스터")
                .name("test")
                .build();
        profile = MemberProfile.builder()
                .member(member)
                .nickname("테스트닉네임")
                .build();
    }

    @Test
    @DisplayName("getTitleList()는 기본, 보유, 미보유 칭호를 분리해서 반환한다")
    void getTitleList_기본_보유_미보유_분리() {
        Title defaultTitle = title(1L, "밸런스 새싹", TitleAcquisitionType.DEFAULT, 0L, null);
        Title ownedTitle = title(2L, "싸움 구경꾼", TitleAcquisitionType.ACHIEVEMENT, 0L, "투표 10회 참여");
        Title pointTitle = title(3L, "선택의 신", TitleAcquisitionType.POINT_PURCHASE, 300L, null);
        Title seasonTitle = title(4L, "2026 봄 논쟁왕", TitleAcquisitionType.SEASON, 0L, "시즌한정");
        MemberProfileTitle ownedProfileTitle = MemberProfileTitle.builder()
                .memberProfile(profile)
                .title(ownedTitle)
                .build();
        ownedProfileTitle.equip();

        when(memberProfileRepository.findByMemberId(1L)).thenReturn(Optional.of(profile));
        when(titleRepository.findAllByActiveTrueOrderByDisplayOrderAscIdAsc())
                .thenReturn(List.of(defaultTitle, ownedTitle, pointTitle, seasonTitle));
        when(memberProfileTitleRepository.findAllByMemberProfileMemberId(1L))
                .thenReturn(List.of(ownedProfileTitle));
        when(memberProfileTitleRepository.saveAll(org.mockito.ArgumentMatchers.anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = titleService.getTitleList(1L);

        assertThat(response.defaultTitles()).hasSize(1);
        assertThat(response.defaultTitles().get(0).title()).isEqualTo("밸런스 새싹");
        assertThat(response.defaultTitles().get(0).owned()).isTrue();
        assertThat(response.defaultTitles().get(0).locked()).isFalse();

        assertThat(response.ownedTitles()).hasSize(1);
        assertThat(response.ownedTitles().get(0).title()).isEqualTo("싸움 구경꾼");
        assertThat(response.ownedTitles().get(0).equipped()).isTrue();

        assertThat(response.lockedTitles()).hasSize(2);
        assertThat(response.lockedTitles().get(0).title()).isEqualTo("선택의 신");
        assertThat(response.lockedTitles().get(0).lockReason()).isEqualTo("300P 필요");
        assertThat(response.lockedTitles().get(1).title()).isEqualTo("2026 봄 논쟁왕");
        assertThat(response.lockedTitles().get(1).lockReason()).isEqualTo("시즌한정");
    }

    @Test
    @DisplayName("getTitleList()는 누락된 기본 칭호를 자동 지급한다")
    void getTitleList_기본칭호_자동지급() {
        Title defaultTitle = title(1L, "밸런스 새싹", TitleAcquisitionType.DEFAULT, 0L, null);

        when(memberProfileRepository.findByMemberId(1L)).thenReturn(Optional.of(profile));
        when(titleRepository.findAllByActiveTrueOrderByDisplayOrderAscIdAsc()).thenReturn(List.of(defaultTitle));
        when(memberProfileTitleRepository.findAllByMemberProfileMemberId(1L)).thenReturn(List.of());
        when(memberProfileTitleRepository.saveAll(org.mockito.ArgumentMatchers.anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = titleService.getTitleList(1L);

        assertThat(response.defaultTitles()).hasSize(1);
        assertThat(response.defaultTitles().get(0).owned()).isTrue();
        verify(memberProfileTitleRepository).saveAll(org.mockito.ArgumentMatchers.argThat(savedTitles ->
                containsOnlyTitle(savedTitles, defaultTitle.getId())
        ));
    }

    @Test
    @DisplayName("equipTitle()은 기존 장착 칭호를 해제하고 선택한 칭호를 장착한다")
    void equipTitle_기존칭호해제_선택칭호장착() {
        MemberProfileTitle equippedTitle = equippedProfileTitle(1L, "기존 칭호");
        MemberProfileTitle targetTitle = profileTitle(2L, "새 칭호");

        when(memberProfileRepository.findByMemberId(1L)).thenReturn(Optional.of(profile));
        when(memberProfileTitleRepository.findByMemberProfileMemberIdAndTitleId(1L, 2L))
                .thenReturn(Optional.of(targetTitle));
        when(memberProfileTitleRepository.findAllByMemberProfileMemberIdAndEquippedTrue(1L))
                .thenReturn(List.of(equippedTitle));

        var response = titleService.equipTitle(1L, 2L);

        assertThat(equippedTitle.isEquipped()).isFalse();
        assertThat(targetTitle.isEquipped()).isTrue();
        assertThat(response.titleId()).isEqualTo(2L);
        assertThat(response.title()).isEqualTo("새 칭호");
        assertThat(response.equipped()).isTrue();
    }

    @Test
    @DisplayName("equipTitle()은 보유하지 않은 칭호를 장착할 수 없다")
    void equipTitle_보유하지않은칭호_예외() {
        when(memberProfileRepository.findByMemberId(1L)).thenReturn(Optional.of(profile));
        when(memberProfileTitleRepository.findByMemberProfileMemberIdAndTitleId(1L, 99L))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> titleService.equipTitle(1L, 99L)
        );

        assertThat(exception.getMessage()).isEqualTo("보유하지 않은 칭호입니다.");
        verify(memberProfileTitleRepository, never()).findAllByMemberProfileMemberIdAndEquippedTrue(1L);
    }

    @Test
    @DisplayName("equipTitle()은 프로필이 없으면 예외를 던진다")
    void equipTitle_프로필없음_예외() {
        when(memberProfileRepository.findByMemberId(1L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> titleService.equipTitle(1L, 1L)
        );

        assertThat(exception.getMessage()).isEqualTo("프로필이 존재하지 않습니다.");
        verify(memberProfileTitleRepository, never()).findByMemberProfileMemberIdAndTitleId(1L, 1L);
    }

    private MemberProfileTitle equippedProfileTitle(Long titleId, String titleName) {
        MemberProfileTitle profileTitle = profileTitle(titleId, titleName);
        profileTitle.equip();
        return profileTitle;
    }

    private MemberProfileTitle profileTitle(Long titleId, String titleName) {
        Title title = title(titleId, titleName, TitleAcquisitionType.DEFAULT, 0L, null);

        return MemberProfileTitle.builder()
                .memberProfile(profile)
                .title(title)
                .build();
    }

    private Title title(
            Long titleId,
            String titleName,
            TitleAcquisitionType acquisitionType,
            long price,
            String requirementText
    ) {
        return Title.builder()
                .id(titleId)
                .code("TITLE_" + titleId)
                .name(titleName)
                .tier(TitleTier.BASIC)
                .acquisitionType(acquisitionType)
                .price(price)
                .requirementText(requirementText)
                .build();
    }

    private boolean containsOnlyTitle(Iterable<MemberProfileTitle> savedTitles, Long titleId) {
        int count = 0;
        boolean matched = false;
        for (MemberProfileTitle savedTitle : savedTitles) {
            count++;
            matched = savedTitle.getTitle().getId().equals(titleId);
        }
        return count == 1 && matched;
    }
}
