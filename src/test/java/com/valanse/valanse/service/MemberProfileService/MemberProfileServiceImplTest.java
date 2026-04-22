package com.valanse.valanse.service.MemberProfileService;

import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.MemberProfile;
import com.valanse.valanse.domain.enums.*;
import com.valanse.valanse.dto.MemberProfile.MemberProfileRequest;
import com.valanse.valanse.repository.MemberProfileRepository;
import com.valanse.valanse.repository.MemberRepository;
import com.valanse.valanse.service.PointService.PointService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberProfileServiceImplTest {

    @InjectMocks
    private MemberProfileServiceImpl memberProfileService;

    @Mock
    private MemberProfileRepository memberProfileRepository;

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private PointService pointService;

    private Member member;

    @BeforeEach
    void setupSecurityContext() {
        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getName()).thenReturn("1");
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        member = Member.builder()
                .email("test@email.com")
                .nickname("테스터")
                .name("test")
                .role(Role.USER)
                .build();
    }

    // ───────────────────────────────────────────────
    // [핵심] soft delete 회원 닉네임 재사용 시나리오
    // ───────────────────────────────────────────────

    @Test
    @DisplayName("[핵심] 탈퇴한 회원의 닉네임은 신규 가입자가 사용할 수 있어야 한다")
    void 탈퇴회원_닉네임_신규회원이_재사용_가능() {
        // given
        MemberProfileRequest request = new MemberProfileRequest(
                "탈퇴한닉네임",  // soft delete된 회원이 쓰던 닉네임
                Gender.MALE,
                Age.TWENTY,
                MbtiIe.E,
                MbtiTf.T,
                "ENTP"
        );

        when(memberRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(member));
        when(memberProfileRepository.findByMemberId(1L))
                .thenReturn(Optional.empty()); // 신규 가입자 (프로필 없음)
        // soft delete된 회원의 닉네임은 deletedAt IS NULL 조건에서 걸리지 않음
        when(memberProfileRepository.existsByNicknameAndDeletedAtIsNull("탈퇴한닉네임"))
                .thenReturn(false);

        // when & then: 예외 없이 정상 저장
        assertDoesNotThrow(() -> memberProfileService.saveOrUpdateProfile(request));
        verify(memberProfileRepository, times(1)).save(any(MemberProfile.class));
    }

    @Test
    @DisplayName("[핵심] 활성 회원이 사용 중인 닉네임은 중복으로 막혀야 한다")
    void 활성회원_닉네임_중복_차단() {
        // given
        MemberProfileRequest request = new MemberProfileRequest(
                "활성회원닉네임",
                Gender.MALE,
                Age.TWENTY,
                MbtiIe.E,
                MbtiTf.T,
                "ENTP"
        );

        when(memberRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(member));
        when(memberProfileRepository.findByMemberId(1L))
                .thenReturn(Optional.empty());
        // 활성 회원이 이미 사용 중
        when(memberProfileRepository.existsByNicknameAndDeletedAtIsNull("활성회원닉네임"))
                .thenReturn(true);

        // when & then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> memberProfileService.saveOrUpdateProfile(request)
        );
        assertThat(exception.getMessage()).isEqualTo("이미 사용 중인 닉네임입니다.");
        verify(memberProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("[핵심] 프로필 수정 시 탈퇴한 회원의 닉네임으로 변경 가능해야 한다")
    void 프로필수정시_탈퇴회원_닉네임으로_변경_가능() {
        // given
        MemberProfile existingProfile = MemberProfile.builder()
                .member(member)
                .nickname("기존닉네임")
                .gender(Gender.MALE)
                .age(Age.TWENTY)
                .mbtiIe(MbtiIe.E)
                .mbtiTf(MbtiTf.T)
                .mbti("ENTP")
                .build();

        MemberProfileRequest request = new MemberProfileRequest(
                "탈퇴한닉네임",  // 탈퇴 회원이 쓰던 닉네임으로 변경 시도
                Gender.MALE,
                Age.TWENTY,
                MbtiIe.E,
                MbtiTf.T,
                "ENTP"
        );

        when(memberRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(member));
        when(memberProfileRepository.findByMemberId(1L))
                .thenReturn(Optional.of(existingProfile));
        when(memberProfileRepository.existsByNicknameAndDeletedAtIsNull("탈퇴한닉네임"))
                .thenReturn(false); // 탈퇴 회원 닉네임이므로 null 아닌 deletedAt 갖고 있음

        // when & then
        assertDoesNotThrow(() -> memberProfileService.saveOrUpdateProfile(request));
        assertThat(existingProfile.getNickname()).isEqualTo("탈퇴한닉네임");
        verify(memberProfileRepository, times(1)).save(existingProfile);
    }

    @Test
    @DisplayName("[핵심] isAvailableNickname은 soft delete된 회원 닉네임을 사용 가능으로 반환해야 한다")
    void isAvailableNickname_탈퇴회원_닉네임은_사용가능() {
        when(memberProfileRepository.existsByNicknameAndDeletedAtIsNull("탈퇴한닉네임"))
                .thenReturn(false);

        assertThat(memberProfileService.isAvailableNickname("탈퇴한닉네임")).isTrue();
    }

    @Test
    @DisplayName("[핵심] isAvailableNickname은 활성 회원 닉네임을 사용 불가로 반환해야 한다")
    void isAvailableNickname_활성회원_닉네임은_사용불가() {
        when(memberProfileRepository.existsByNicknameAndDeletedAtIsNull("활성닉네임"))
                .thenReturn(true);

        assertThat(memberProfileService.isAvailableNickname("활성닉네임")).isFalse();
    }

    // ───────────────────────────────────────────────
    // 기존 시나리오
    // ───────────────────────────────────────────────

    @Test
    @DisplayName("닉네임 변경 없이 MBTI만 수정 - 중복 체크 없이 정상 저장")
    void 닉네임_변경없이_MBTI만_수정_성공() {
        MemberProfile existingProfile = MemberProfile.builder()
                .member(member).nickname("기존닉네임")
                .gender(Gender.MALE).age(Age.TWENTY)
                .mbtiIe(MbtiIe.E).mbtiTf(MbtiTf.T).mbti("ENTP")
                .build();

        MemberProfileRequest request = new MemberProfileRequest(
                "기존닉네임", Gender.MALE, Age.TWENTY, MbtiIe.I, MbtiTf.F, "INFP"
        );

        when(memberRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(member));
        when(memberProfileRepository.findByMemberId(1L)).thenReturn(Optional.of(existingProfile));

        memberProfileService.saveOrUpdateProfile(request);

        verify(memberProfileRepository, never()).existsByNicknameAndDeletedAtIsNull(any());
        verify(memberProfileRepository, times(1)).save(any(MemberProfile.class));
        assertThat(existingProfile.getMbti()).isEqualTo("INFP");
    }

    @Test
    @DisplayName("MBTI 3글자 입력 - 'MBTI는 4자리여야 합니다' 에러")
    void MBTI_불완전_입력_실패() {
        MemberProfileRequest request = new MemberProfileRequest(
                "테스트닉네임", Gender.MALE, Age.TWENTY, MbtiIe.E, MbtiTf.T, "ENT"
        );
        when(memberRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(member));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> memberProfileService.saveOrUpdateProfile(request));
        assertThat(ex.getMessage()).isEqualTo("MBTI는 4자리여야 합니다 (예: ENFP)");
        verify(memberProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("MBTI null 입력 - 'MBTI는 4자리여야 합니다' 에러")
    void MBTI_null_입력_실패() {
        MemberProfileRequest request = new MemberProfileRequest(
                "테스트닉네임", Gender.MALE, Age.TWENTY, MbtiIe.E, MbtiTf.T, null
        );
        when(memberRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(member));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> memberProfileService.saveOrUpdateProfile(request));
        assertThat(ex.getMessage()).isEqualTo("MBTI는 4자리여야 합니다 (예: ENFP)");
    }

    @Test
    @DisplayName("IE만 선택하고 TF 미선택 - 'MBTI를 모두 선택해주세요' 에러")
    void MBTI_TF_미선택_실패() {
        MemberProfileRequest request = new MemberProfileRequest(
                "테스트닉네임", Gender.MALE, Age.TWENTY, MbtiIe.E, null, "ENTP"
        );
        when(memberRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(member));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> memberProfileService.saveOrUpdateProfile(request));
        assertThat(ex.getMessage()).isEqualTo("MBTI를 모두 선택해주세요");
        verify(memberProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("TF만 선택하고 IE 미선택 - 'MBTI를 모두 선택해주세요' 에러")
    void MBTI_IE_미선택_실패() {
        MemberProfileRequest request = new MemberProfileRequest(
                "테스트닉네임", Gender.MALE, Age.TWENTY, null, MbtiTf.T, "ENTP"
        );
        when(memberRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(member));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> memberProfileService.saveOrUpdateProfile(request));
        assertThat(ex.getMessage()).isEqualTo("MBTI를 모두 선택해주세요");
    }

    @Test
    @DisplayName("닉네임 변경 시 활성 회원과 중복 - '이미 사용 중인 닉네임입니다' 에러")
    void 닉네임_변경시_활성회원_중복_에러() {
        MemberProfile existingProfile = MemberProfile.builder()
                .member(member).nickname("기존닉네임")
                .gender(Gender.MALE).age(Age.TWENTY)
                .mbtiIe(MbtiIe.E).mbtiTf(MbtiTf.T).mbti("ENTP")
                .build();

        MemberProfileRequest request = new MemberProfileRequest(
                "중복닉네임", Gender.MALE, Age.TWENTY, MbtiIe.E, MbtiTf.T, "ENTP"
        );

        when(memberRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(member));
        when(memberProfileRepository.findByMemberId(1L)).thenReturn(Optional.of(existingProfile));
        when(memberProfileRepository.existsByNicknameAndDeletedAtIsNull("중복닉네임")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> memberProfileService.saveOrUpdateProfile(request));
        assertThat(ex.getMessage()).isEqualTo("이미 사용 중인 닉네임입니다.");
        verify(memberProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("닉네임 변경 - 중복 없음 - 정상 저장")
    void 닉네임_변경_중복없음_성공() {
        MemberProfile existingProfile = MemberProfile.builder()
                .member(member).nickname("기존닉네임")
                .gender(Gender.MALE).age(Age.TWENTY)
                .mbtiIe(MbtiIe.E).mbtiTf(MbtiTf.T).mbti("ENTP")
                .build();

        MemberProfileRequest request = new MemberProfileRequest(
                "새닉네임", Gender.MALE, Age.TWENTY, MbtiIe.E, MbtiTf.T, "ENTP"
        );

        when(memberRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(member));
        when(memberProfileRepository.findByMemberId(1L)).thenReturn(Optional.of(existingProfile));
        when(memberProfileRepository.existsByNicknameAndDeletedAtIsNull("새닉네임")).thenReturn(false);

        memberProfileService.saveOrUpdateProfile(request);

        verify(memberProfileRepository, times(1)).existsByNicknameAndDeletedAtIsNull("새닉네임");
        verify(memberProfileRepository, times(1)).save(any(MemberProfile.class));
        assertThat(existingProfile.getNickname()).isEqualTo("새닉네임");
    }
}
