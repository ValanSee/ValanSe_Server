package com.valanse.valanse.service.TitleService;

import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.MemberProfile;
import com.valanse.valanse.domain.MemberProfileTitle;
import com.valanse.valanse.domain.Title;
import com.valanse.valanse.domain.enums.TitleAcquisitionType;
import com.valanse.valanse.domain.enums.TitleTier;
import com.valanse.valanse.repository.MemberProfileRepository;
import com.valanse.valanse.repository.MemberProfileTitleRepository;
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
        Title title = Title.builder()
                .id(titleId)
                .code("TITLE_" + titleId)
                .name(titleName)
                .tier(TitleTier.BASIC)
                .acquisitionType(TitleAcquisitionType.DEFAULT)
                .build();

        return MemberProfileTitle.builder()
                .memberProfile(profile)
                .title(title)
                .build();
    }
}
