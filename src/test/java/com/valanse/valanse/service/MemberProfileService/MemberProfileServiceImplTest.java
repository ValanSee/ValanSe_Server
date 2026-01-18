package com.valanse.valanse.service.MemberProfileService;

import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.MemberProfile;
import com.valanse.valanse.domain.enums.*;
import com.valanse.valanse.dto.MemberProfile.MemberProfileRequest;
import com.valanse.valanse.repository.MemberProfileRepository;
import com.valanse.valanse.repository.MemberRepository;
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

/**
 * Issue #110: MBTI 프로필 수정 검증 로직 테스트
 *
 * 테스트 시나리오:
 * 1. 닉네임 변경 없이 MBTI만 수정 - 중복 체크 없이 정상 저장
 * 2. MBTI 2~3글자만 입력 - "MBTI는 4자리여야 합니다" 에러
 * 3. IE만 선택하고 TF 미선택 - "MBTI를 모두 선택해주세요" 에러
 * 4. 닉네임 중복 (실제 중복) - "이미 사용 중인 닉네임입니다" 에러
 * 5. 신규 프로필 생성 시 닉네임 중복 - "이미 사용 중인 닉네임입니다" 에러
 * 6. 정상적인 MBTI 4글자 입력 - 정상 저장
 */
@ExtendWith(MockitoExtension.class)
class MemberProfileServiceImplTest_Issue110 {

    @InjectMocks
    private MemberProfileServiceImpl memberProfileService;

    @Mock
    private MemberProfileRepository memberProfileRepository;

    @Mock
    private MemberRepository memberRepository;

    private Member member;

    @BeforeEach
    void setupSecurityContext() {
        // SecurityContext 설정 (userId = 1)
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("1");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Member 객체 초기화
        member = Member.builder()
                .id(1L)
                .email("test@email.com")
                .nickname("테스터")
                .name("test")
                .role(Role.USER)
                .build();
    }

    @Test
    @DisplayName("테스트 1: 닉네임 변경 없이 MBTI만 수정 - 중복 체크 없이 정상 저장")
    void 닉네임_변경없이_MBTI만_수정_성공() {
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
                "기존닉네임",  // 닉네임 변경 없음
                Gender.MALE,
                Age.TWENTY,
                MbtiIe.I,     // IE 변경: E -> I
                MbtiTf.F,     // TF 변경: T -> F
                "INFP"        // MBTI 변경
        );

        // stub
        when(memberRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(member));
        when(memberProfileRepository.findByMemberId(1L))
                .thenReturn(Optional.of(existingProfile));

        // when
        memberProfileService.saveOrUpdateProfile(request);

        // then
        // 닉네임이 변경되지 않았으므로 중복 체크를 호출하지 않아야 함
        verify(memberProfileRepository, never()).existsByNickname(any());

        // save는 1회 호출
        verify(memberProfileRepository, times(1)).save(any(MemberProfile.class));

        // 프로필 업데이트 확인
        assertThat(existingProfile.getMbti()).isEqualTo("INFP");
        assertThat(existingProfile.getMbtiIe()).isEqualTo(MbtiIe.I);
        assertThat(existingProfile.getMbtiTf()).isEqualTo(MbtiTf.F);
    }

    @Test
    @DisplayName("테스트 2: MBTI 2~3글자만 입력 - 'MBTI는 4자리여야 합니다' 에러")
    void MBTI_불완전_입력_실패() {
        // given
        MemberProfileRequest request = new MemberProfileRequest(
                "테스트닉네임",
                Gender.MALE,
                Age.TWENTY,
                MbtiIe.E,
                MbtiTf.T,
                "ENT"  // 3글자만 입력
        );

        // stub
        when(memberRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(member));

        // when & then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> memberProfileService.saveOrUpdateProfile(request)
        );

        assertThat(exception.getMessage()).isEqualTo("MBTI는 4자리여야 합니다 (예: ENFP)");

        // 저장이 호출되지 않아야 함
        verify(memberProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("테스트 2-1: MBTI null 입력 - 'MBTI는 4자리여야 합니다' 에러")
    void MBTI_null_입력_실패() {
        // given
        MemberProfileRequest request = new MemberProfileRequest(
                "테스트닉네임",
                Gender.MALE,
                Age.TWENTY,
                MbtiIe.E,
                MbtiTf.T,
                null  // null 입력
        );

        // stub
        when(memberRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(member));

        // when & then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> memberProfileService.saveOrUpdateProfile(request)
        );

        assertThat(exception.getMessage()).isEqualTo("MBTI는 4자리여야 합니다 (예: ENFP)");
    }

    @Test
    @DisplayName("테스트 3: IE만 선택하고 TF 미선택 - 'MBTI를 모두 선택해주세요' 에러")
    void MBTI_일부만_선택_실패() {
        // given
        MemberProfileRequest request = new MemberProfileRequest(
                "테스트닉네임",
                Gender.MALE,
                Age.TWENTY,
                MbtiIe.E,  // IE만 선택
                null,      // TF 미선택
                "ENTP"
        );

        // stub
        when(memberRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(member));

        // when & then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> memberProfileService.saveOrUpdateProfile(request)
        );

        assertThat(exception.getMessage()).isEqualTo("MBTI를 모두 선택해주세요");

        // 저장이 호출되지 않아야 함
        verify(memberProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("테스트 3-1: TF만 선택하고 IE 미선택 - 'MBTI를 모두 선택해주세요' 에러")
    void MBTI_IE만_미선택_실패() {
        // given
        MemberProfileRequest request = new MemberProfileRequest(
                "테스트닉네임",
                Gender.MALE,
                Age.TWENTY,
                null,      // IE 미선택
                MbtiTf.T,  // TF만 선택
                "ENTP"
        );

        // stub
        when(memberRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(member));

        // when & then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> memberProfileService.saveOrUpdateProfile(request)
        );

        assertThat(exception.getMessage()).isEqualTo("MBTI를 모두 선택해주세요");
    }

    @Test
    @DisplayName("테스트 4: 닉네임 변경 시 실제 중복 - '이미 사용 중인 닉네임입니다' 에러")
    void 닉네임_변경시_중복_에러() {
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
                "중복닉네임",  // 다른 사람이 이미 사용 중
                Gender.MALE,
                Age.TWENTY,
                MbtiIe.E,
                MbtiTf.T,
                "ENTP"
        );

        // stub
        when(memberRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(member));
        when(memberProfileRepository.findByMemberId(1L))
                .thenReturn(Optional.of(existingProfile));
        when(memberProfileRepository.existsByNickname("중복닉네임"))
                .thenReturn(true);  // 중복됨

        // when & then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> memberProfileService.saveOrUpdateProfile(request)
        );

        assertThat(exception.getMessage()).isEqualTo("이미 사용 중인 닉네임입니다.");

        // 중복 체크는 호출되어야 함
        verify(memberProfileRepository, times(1)).existsByNickname("중복닉네임");

        // 저장은 호출되지 않아야 함
        verify(memberProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("테스트 5: 신규 프로필 생성 시 닉네임 중복 - '이미 사용 중인 닉네임입니다' 에러")
    void 신규_프로필_닉네임_중복_에러() {
        // given
        MemberProfileRequest request = new MemberProfileRequest(
                "중복닉네임",
                Gender.MALE,
                Age.TWENTY,
                MbtiIe.E,
                MbtiTf.T,
                "ENTP"
        );

        // stub
        when(memberRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(member));
        when(memberProfileRepository.findByMemberId(1L))
                .thenReturn(Optional.empty());  // 기존 프로필 없음 (신규)
        when(memberProfileRepository.existsByNickname("중복닉네임"))
                .thenReturn(true);  // 중복됨

        // when & then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> memberProfileService.saveOrUpdateProfile(request)
        );

        assertThat(exception.getMessage()).isEqualTo("이미 사용 중인 닉네임입니다.");

        // 신규 생성 시에도 중복 체크 호출
        verify(memberProfileRepository, times(1)).existsByNickname("중복닉네임");

        // 저장은 호출되지 않아야 함
        verify(memberProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("테스트 6: 정상적인 MBTI 4글자 입력 - 정상 저장")
    void 정상_프로필_저장_성공() {
        // given
        MemberProfileRequest request = new MemberProfileRequest(
                "새로운닉네임",
                Gender.FEMALE,
                Age.THIRTY,
                MbtiIe.I,
                MbtiTf.F,
                "INFP"  // 정상적인 4글자
        );

        MemberProfile newProfile = MemberProfile.builder()
                .member(member)
                .nickname(request.nickname())
                .gender(request.gender())
                .age(request.age())
                .mbtiIe(request.mbtiIe())
                .mbtiTf(request.mbtiTf())
                .mbti(request.mbti())
                .build();

        // stub
        when(memberRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(member));
        when(memberProfileRepository.findByMemberId(1L))
                .thenReturn(Optional.empty());  // 신규 생성
        when(memberProfileRepository.existsByNickname("새로운닉네임"))
                .thenReturn(false);  // 중복 없음
        when(memberProfileRepository.save(any(MemberProfile.class)))
                .thenReturn(newProfile);

        // when
        memberProfileService.saveOrUpdateProfile(request);

        // then
        // 중복 체크 호출
        verify(memberProfileRepository, times(1)).existsByNickname("새로운닉네임");

        // 저장 1회 호출
        verify(memberProfileRepository, times(1)).save(any(MemberProfile.class));
    }

    @Test
    @DisplayName("테스트 7: 닉네임 동일, MBTI 정상 변경 - 정상 저장")
    void 닉네임_동일_MBTI_변경_성공() {
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
                "기존닉네임",  // 닉네임 변경 없음
                Gender.MALE,
                Age.TWENTY,
                MbtiIe.I,
                MbtiTf.F,
                "INFP"  // MBTI만 변경
        );

        // stub
        when(memberRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(member));
        when(memberProfileRepository.findByMemberId(1L))
                .thenReturn(Optional.of(existingProfile));

        // when
        memberProfileService.saveOrUpdateProfile(request);

        // then
        // 닉네임 동일하므로 중복 체크 호출 안됨
        verify(memberProfileRepository, never()).existsByNickname(any());

        // 저장은 1회 호출
        verify(memberProfileRepository, times(1)).save(any(MemberProfile.class));

        // MBTI가 변경되었는지 확인
        assertThat(existingProfile.getMbti()).isEqualTo("INFP");
    }

    @Test
    @DisplayName("테스트 8: 닉네임 변경하면서 중복 없음 - 정상 저장")
    void 닉네임_변경_중복없음_성공() {
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
                "새닉네임",  // 닉네임 변경
                Gender.MALE,
                Age.TWENTY,
                MbtiIe.E,
                MbtiTf.T,
                "ENTP"
        );

        // stub
        when(memberRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(member));
        when(memberProfileRepository.findByMemberId(1L))
                .thenReturn(Optional.of(existingProfile));
        when(memberProfileRepository.existsByNickname("새닉네임"))
                .thenReturn(false);  // 중복 없음

        // when
        memberProfileService.saveOrUpdateProfile(request);

        // then
        // 닉네임이 변경되었으므로 중복 체크 호출됨
        verify(memberProfileRepository, times(1)).existsByNickname("새닉네임");

        // 저장은 1회 호출
        verify(memberProfileRepository, times(1)).save(any(MemberProfile.class));

        // 닉네임이 변경되었는지 확인
        assertThat(existingProfile.getNickname()).isEqualTo("새닉네임");
    }
}